# RDS master ("postgres" superuser) password - generated, never typed in by
# a human, rotated by changing this resource and applying.
resource "random_password" "rds_master" {
  length  = 32
  special = false # avoid chars Postgres/JDBC URLs need escaping
}

resource "aws_secretsmanager_secret" "rds_master_password" {
  name = "adoptu/rds-master-password"
}

resource "aws_secretsmanager_secret_version" "rds_master_password" {
  secret_id     = aws_secretsmanager_secret.rds_master_password.id
  secret_string = random_password.rds_master.result
}

# Application-level "adoptu" Postgres role password (see variables.tf for
# why this isn't auto-generated). Replaces the plaintext ADOPTU_DB_PASSWORD
# env var baked into the live task definition.
resource "aws_secretsmanager_secret" "db_app_password" {
  name = "adoptu/db-password"
}

resource "aws_secretsmanager_secret_version" "db_app_password" {
  secret_id     = aws_secretsmanager_secret.db_app_password.id
  secret_string = var.db_app_password
}
