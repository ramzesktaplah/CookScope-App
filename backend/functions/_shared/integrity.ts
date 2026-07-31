// Server-side Play Integrity verification for CookScope Edge Functions.
//
// Verifies the integrity token sent by the Android client by calling Google's
// Play Integrity REST API. Uses a Google service account to obtain an OAuth2
// access token for the decodeIntegrityToken endpoint.
//
// Behavior:
//   - If no X-Integrity-Token header is present:
//     - If INTEGRITY_ENFORCE is set, throw IntegrityError (fail-closed).
//     - Otherwise, return OK (fail-open during the transition grace period).
//   - If a token is present, decode it via Google's API and check:
//     - appRecognitionVerdict == "PLAY_RECOGNIZED"
//     - deviceIntegrity includes "MEETS_DEVICE_INTEGRITY"
//
// Required edge function secrets:
//   - GOOGLE_CLOUD_PROJECT_NUMBER: numeric Google Cloud project number
//   - GOOGLE_SERVICE_ACCOUNT_JSON: JSON key for a service account with
//     Play Integrity API access
//   - INTEGRITY_ENFORCE (optional): set to "true" to fail-closed when no
//     token is present. Leave unset during the transition grace period.

/** Error thrown when the integrity check fails. */
export class IntegrityError extends Error {
  constructor(message: string) {
    super(message);
    this.name = "IntegrityError";
  }
}

const PLAY_INTEGRITY_API =
  "https://playintegrity.googleapis.com/v1/projects";

/** Cache the service account access token to avoid re-minting on every call. */
let cachedAccessToken: { token: string; expiresAt: number } | null = null;

/**
 * Mint a Google OAuth2 access token from the service account JSON using
 * JWT exchange (RFC 7523). Returns the access token string.
 */
async function getAccessToken(): Promise<string> {
  if (cachedAccessToken && Date.now() < cachedAccessToken.expiresAt) {
    return cachedAccessToken.token;
  }

  const saJson = Deno.env.get("GOOGLE_SERVICE_ACCOUNT_JSON");
  if (!saJson) {
    throw new IntegrityError("Integrity verification not configured.");
  }

  const sa = JSON.parse(saJson);
  const now = Math.floor(Date.now() / 1000);
  const jwtHeader = { alg: "RS256", typ: "JWT" };
  const jwtPayload = {
    iss: sa.client_email,
    scope: "https://www.googleapis.com/auth/playintegrity",
    aud: "https://oauth2.googleapis.com/token",
    exp: now + 3600,
    iat: now,
  };

  // Build the unsigned JWT.
  const encoder = new TextEncoder();
  const headerB64 = base64UrlEncode(encoder.encode(JSON.stringify(jwtHeader)));
  const payloadB64 = base64UrlEncode(encoder.encode(JSON.stringify(jwtPayload)));
  const unsignedJwt = `${headerB64}.${payloadB64}`;

  // Sign with the service account private key using Web Crypto API.
  const privateKey = await importRsaPrivateKey(sa.private_key);
  const signature = await crypto.subtle.sign(
    "RSASSA-PKCS1-v1_5",
    privateKey,
    encoder.encode(unsignedJwt),
  );
  const signatureB64 = base64UrlEncode(new Uint8Array(signature));
  const signedJwt = `${unsignedJwt}.${signatureB64}`;

  // Exchange the JWT for an access token.
  const tokenResp = await fetch("https://oauth2.googleapis.com/token", {
    method: "POST",
    headers: { "Content-Type": "application/x-www-form-urlencoded" },
    body: new URLSearchParams({
      grant_type: "urn:ietf:params:oauth:grant-type:jwt-bearer",
      assertion: signedJwt,
    }),
  });

  if (!tokenResp.ok) {
    throw new IntegrityError("Integrity verification service unavailable.");
  }

  const tokenData = await tokenResp.json();
  const accessToken = tokenData.access_token as string;
  const expiresIn = (tokenData.expires_in as number) ?? 3600;

  cachedAccessToken = {
    token: accessToken,
    expiresAt: Date.now() + (expiresIn - 60) * 1000,
  };

  return accessToken;
}

/**
 * Verify the Play Integrity token on the request.
 *
 * Call this after requireAuth() and enforceRateLimit() in each edge function.
 * Throws IntegrityError if the verification fails or is enforced but no token
 * is present. Returns silently if the check passes or is in fail-open mode.
 *
 * @param req The incoming Request — reads X-Integrity-Token and X-Integrity-Hash headers.
 * @param userId The authenticated user's ID (not currently used in the verdict
 *   check, but available for future per-user binding).
 */
export async function verifyIntegrity(
  req: Request,
  _userId: string,
): Promise<void> {
  const token = req.headers.get("X-Integrity-Token");
  const enforce = Deno.env.get("INTEGRITY_ENFORCE") === "true";

  // No token present — fail-open or fail-closed based on config.
  if (!token) {
    if (enforce) {
      throw new IntegrityError(
        "App verification required. Please use the official app from Google Play.",
      );
    }
    return; // fail-open (grace period or not yet configured)
  }

  const projectNumber = Deno.env.get("GOOGLE_CLOUD_PROJECT_NUMBER");
  if (!projectNumber) {
    // Backend not configured for integrity verification — fail-open.
    return;
  }

  try {
    const accessToken = await getAccessToken();

    const decodeResp = await fetch(
      `${PLAY_INTEGRITY_API}/${projectNumber}:decodeIntegrityToken`,
      {
        method: "POST",
        headers: {
          Authorization: `Bearer ${accessToken}`,
          "Content-Type": "application/json",
        },
        body: JSON.stringify({ integrity_token: token }),
      },
    );

    if (!decodeResp.ok) {
      // Google API error — fail-open to avoid blocking legitimate users
      // during Google outages. Log for monitoring.
      console.error("integrity: decodeIntegrityToken failed:", decodeResp.status);
      return;
    }

    const decoded = await decodeResp.json();
    const verdict = decoded?.tokenPayloadExternal?.appIntegrity?.appRecognitionVerdict;
    const deviceIntegrity = decoded?.tokenPayloadExternal?.deviceIntegrity ?? {};

    // Accept if the app is recognized AND the device meets basic integrity.
    if (verdict !== "PLAY_RECOGNIZED") {
      throw new IntegrityError(
        "App not recognized. Please install the official app from Google Play.",
      );
    }

    // MEETS_DEVICE_INTEGRITY means the device is not rooted/compromised.
    // MEETS_STRONG_INTEGRITY is stricter but less common — we accept either.
    const hasDeviceIntegrity =
      "MEETS_DEVICE_INTEGRITY" in deviceIntegrity ||
      "MEETS_STRONG_INTEGRITY" in deviceIntegrity;

    if (!hasDeviceIntegrity) {
      throw new IntegrityError(
        "Device verification failed. Please use a trusted device.",
      );
    }
  } catch (err) {
    // Re-throw IntegrityErrors as-is; other errors fail-open.
    if (err instanceof IntegrityError) throw err;
    console.error("integrity: verification error:", err);
    return;
  }
}

// ---- Helpers for RSA JWT signing via Web Crypto API ----

function base64UrlEncode(bytes: Uint8Array): string {
  let binary = "";
  for (const b of bytes) {
    binary += String.fromCharCode(b);
  }
  return btoa(binary)
    .replace(/\+/g, "-")
    .replace(/\//g, "_")
    .replace(/=+$/, "");
}

async function importRsaPrivateKey(pem: string): Promise<CryptoKey> {
  // Strip PEM headers and parse the DER-encoded PKCS#8 key.
  const pemContents = pem
    .replace(/-----BEGIN PRIVATE KEY-----/, "")
    .replace(/-----END PRIVATE KEY-----/, "")
    .replace(/\s/g, "");

  const der = Uint8Array.from(atob(pemContents), (c) => c.charCodeAt(0));

  return crypto.subtle.importKey(
    "pkcs8",
    der,
    { name: "RSASSA-PKCS1-v1_5", hash: "SHA-256" },
    false,
    ["sign"],
  );
}
