CREATE TABLE "users"
(
    "id"            UUID PRIMARY KEY      DEFAULT gen_random_uuid(),
    "name"          varchar(255) NOT NULL,
    "username"      varchar(255) NOT NULL,
    "email"         varchar(255) NOT NULL,
    "password_hash" varchar(255) NOT NULL,
    "active"        boolean      NOT NULL DEFAULT true,
    "img_url"       varchar(2048),
    "created_at"    timestamptz  NOT NULL DEFAULT now(),
    "updated_at"    timestamptz  NOT NULL DEFAULT now(),
    CONSTRAINT "uk_users_username" UNIQUE ("username"),
    CONSTRAINT "uk_users_email" UNIQUE ("email")
);
