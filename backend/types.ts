/* eslint-disable */
// AUTO-GENERATED — DO NOT EDIT
// Run migrations to regenerate.

export type Json =
  | string
  | number
  | boolean
  | null
  | { [key: string]: Json | undefined }
  | Json[]

export type Database = {
  // Allows to automatically instantiate createClient with right options
  // instead of createClient<Database, { PostgrestVersion: 'XX' }>(URL, KEY)
  __InternalSupabase: {
    PostgrestVersion: "14.5"
  }
  public: {
    Tables: {
      chat_messages: {
        Row: {
          content: string
          created_at: string | null
          id: string
          role: string
          user_id: string
        }
        Insert: {
          content: string
          created_at?: string | null
          id?: string
          role: string
          user_id: string
        }
        Update: {
          content?: string
          created_at?: string | null
          id?: string
          role?: string
          user_id?: string
        }
        Relationships: []
      }
      profiles: {
        Row: {
          avatar_url: string | null
          bio: string | null
          created_at: string | null
          display_name: string
          id: string
          plan_type: string
          role: string
          stars_count: number
          stars_given: number
          updated_at: string | null
          username: string
        }
        Insert: {
          avatar_url?: string | null
          bio?: string | null
          created_at?: string | null
          display_name?: string
          id: string
          plan_type?: string
          role?: string
          stars_count?: number
          stars_given?: number
          updated_at?: string | null
          username?: string
        }
        Update: {
          avatar_url?: string | null
          bio?: string | null
          created_at?: string | null
          display_name?: string
          id?: string
          plan_type?: string
          role?: string
          stars_count?: number
          stars_given?: number
          updated_at?: string | null
          username?: string
        }
        Relationships: []
      }
      saved_recipes: {
        Row: {
          created_at: string | null
          id: string
          recipe_cook_time_min: number | null
          recipe_cuisine: string | null
          recipe_difficulty: string | null
          recipe_id: string
          recipe_image_url: string | null
          recipe_title: string
          source: string | null
          user_id: string
        }
        Insert: {
          created_at?: string | null
          id?: string
          recipe_cook_time_min?: number | null
          recipe_cuisine?: string | null
          recipe_difficulty?: string | null
          recipe_id: string
          recipe_image_url?: string | null
          recipe_title: string
          source?: string | null
          user_id: string
        }
        Update: {
          created_at?: string | null
          id?: string
          recipe_cook_time_min?: number | null
          recipe_cuisine?: string | null
          recipe_difficulty?: string | null
          recipe_id?: string
          recipe_image_url?: string | null
          recipe_title?: string
          source?: string | null
          user_id?: string
        }
        Relationships: []
      }
      trending_recipes: {
        Row: {
          cook_time_min: number | null
          created_at: string
          cuisine: string | null
          difficulty: string | null
          for_date: string
          id: string
          image_url: string | null
          recipe_data: Json
          recipe_id: string
          title: string
        }
        Insert: {
          cook_time_min?: number | null
          created_at?: string
          cuisine?: string | null
          difficulty?: string | null
          for_date?: string
          id?: string
          image_url?: string | null
          recipe_data?: Json
          recipe_id: string
          title: string
        }
        Update: {
          cook_time_min?: number | null
          created_at?: string
          cuisine?: string | null
          difficulty?: string | null
          for_date?: string
          id?: string
          image_url?: string | null
          recipe_data?: Json
          recipe_id?: string
          title?: string
        }
        Relationships: []
      }
      user_preferences: {
        Row: {
          allergies: string[] | null
          dietary_tags: string[] | null
          favorite_cuisines: string[] | null
          id: string
          skill_level: string | null
          updated_at: string | null
          user_id: string
        }
        Insert: {
          allergies?: string[] | null
          dietary_tags?: string[] | null
          favorite_cuisines?: string[] | null
          id?: string
          skill_level?: string | null
          updated_at?: string | null
          user_id: string
        }
        Update: {
          allergies?: string[] | null
          dietary_tags?: string[] | null
          favorite_cuisines?: string[] | null
          id?: string
          skill_level?: string | null
          updated_at?: string | null
          user_id?: string
        }
        Relationships: []
      }
    }
    Views: {
      [_ in never]: never
    }
    Functions: {
      [_ in never]: never
    }
    Enums: {
      [_ in never]: never
    }
    CompositeTypes: {
      [_ in never]: never
    }
  }
}

type DatabaseWithoutInternals = Omit<Database, "__InternalSupabase">

type DefaultSchema = DatabaseWithoutInternals[Extract<keyof Database, "public">]

export type Tables<
  DefaultSchemaTableNameOrOptions extends
    | keyof (DefaultSchema["Tables"] & DefaultSchema["Views"])
    | { schema: keyof DatabaseWithoutInternals },
  TableName extends DefaultSchemaTableNameOrOptions extends {
    schema: keyof DatabaseWithoutInternals
  }
    ? keyof (DatabaseWithoutInternals[DefaultSchemaTableNameOrOptions["schema"]]["Tables"] &
        DatabaseWithoutInternals[DefaultSchemaTableNameOrOptions["schema"]]["Views"])
    : never = never,
> = DefaultSchemaTableNameOrOptions extends {
  schema: keyof DatabaseWithoutInternals
}
  ? (DatabaseWithoutInternals[DefaultSchemaTableNameOrOptions["schema"]]["Tables"] &
      DatabaseWithoutInternals[DefaultSchemaTableNameOrOptions["schema"]]["Views"])[TableName] extends {
      Row: infer R
    }
    ? R
    : never
  : DefaultSchemaTableNameOrOptions extends keyof (DefaultSchema["Tables"] &
        DefaultSchema["Views"])
    ? (DefaultSchema["Tables"] &
        DefaultSchema["Views"])[DefaultSchemaTableNameOrOptions] extends {
        Row: infer R
      }
      ? R
      : never
    : never

export type TablesInsert<
  DefaultSchemaTableNameOrOptions extends
    | keyof DefaultSchema["Tables"]
    | { schema: keyof DatabaseWithoutInternals },
  TableName extends DefaultSchemaTableNameOrOptions extends {
    schema: keyof DatabaseWithoutInternals
  }
    ? keyof DatabaseWithoutInternals[DefaultSchemaTableNameOrOptions["schema"]]["Tables"]
    : never = never,
> = DefaultSchemaTableNameOrOptions extends {
  schema: keyof DatabaseWithoutInternals
}
  ? DatabaseWithoutInternals[DefaultSchemaTableNameOrOptions["schema"]]["Tables"][TableName] extends {
      Insert: infer I
    }
    ? I
    : never
  : DefaultSchemaTableNameOrOptions extends keyof DefaultSchema["Tables"]
    ? DefaultSchema["Tables"][DefaultSchemaTableNameOrOptions] extends {
        Insert: infer I
      }
      ? I
      : never
    : never

export type TablesUpdate<
  DefaultSchemaTableNameOrOptions extends
    | keyof DefaultSchema["Tables"]
    | { schema: keyof DatabaseWithoutInternals },
  TableName extends DefaultSchemaTableNameOrOptions extends {
    schema: keyof DatabaseWithoutInternals
  }
    ? keyof DatabaseWithoutInternals[DefaultSchemaTableNameOrOptions["schema"]]["Tables"]
    : never = never,
> = DefaultSchemaTableNameOrOptions extends {
  schema: keyof DatabaseWithoutInternals
}
  ? DatabaseWithoutInternals[DefaultSchemaTableNameOrOptions["schema"]]["Tables"][TableName] extends {
      Update: infer U
    }
    ? U
    : never
  : DefaultSchemaTableNameOrOptions extends keyof DefaultSchema["Tables"]
    ? DefaultSchema["Tables"][DefaultSchemaTableNameOrOptions] extends {
        Update: infer U
      }
      ? U
      : never
    : never

export type Enums<
  DefaultSchemaEnumNameOrOptions extends
    | keyof DefaultSchema["Enums"]
    | { schema: keyof DatabaseWithoutInternals },
  EnumName extends DefaultSchemaEnumNameOrOptions extends {
    schema: keyof DatabaseWithoutInternals
  }
    ? keyof DatabaseWithoutInternals[DefaultSchemaEnumNameOrOptions["schema"]]["Enums"]
    : never = never,
> = DefaultSchemaEnumNameOrOptions extends {
  schema: keyof DatabaseWithoutInternals
}
  ? DatabaseWithoutInternals[DefaultSchemaEnumNameOrOptions["schema"]]["Enums"][EnumName]
  : DefaultSchemaEnumNameOrOptions extends keyof DefaultSchema["Enums"]
    ? DefaultSchema["Enums"][DefaultSchemaEnumNameOrOptions]
    : never

export type CompositeTypes<
  PublicCompositeTypeNameOrOptions extends
    | keyof DefaultSchema["CompositeTypes"]
    | { schema: keyof DatabaseWithoutInternals },
  CompositeTypeName extends PublicCompositeTypeNameOrOptions extends {
    schema: keyof DatabaseWithoutInternals
  }
    ? keyof DatabaseWithoutInternals[PublicCompositeTypeNameOrOptions["schema"]]["CompositeTypes"]
    : never = never,
> = PublicCompositeTypeNameOrOptions extends {
  schema: keyof DatabaseWithoutInternals
}
  ? DatabaseWithoutInternals[PublicCompositeTypeNameOrOptions["schema"]]["CompositeTypes"][CompositeTypeName]
  : PublicCompositeTypeNameOrOptions extends keyof DefaultSchema["CompositeTypes"]
    ? DefaultSchema["CompositeTypes"][PublicCompositeTypeNameOrOptions]
    : never

export const Constants = {
  public: {
    Enums: {},
  },
} as const
