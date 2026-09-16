CREATE TABLE "users"
(
    "id"         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    "first_name" varchar(255) NOT NULL,
    "last_name"  varchar(255) NOT NULL,
    "username"   varchar(255) NOT NULL,
    "password"   varchar(255) NOT NULL,
    "email"      varchar(255) NOT NULL
);