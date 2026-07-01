variable "aws_region" {
  description = "AWS region for all resources except the ACM viewer certificate."
  type        = string
  default     = "us-east-1"
}

variable "aws_profile" {
  description = "Named AWS CLI profile to use (account 174000857825)."
  type        = string
  default     = "adoptu"
}

variable "domain_name" {
  description = "Root domain managed in Route 53 (existing hosted zone)."
  type        = string
  default     = "adopt-u.org"
}

variable "vpc_id" {
  description = "VPC to deploy into. Defaults to discovering the account's default VPC."
  type        = string
  default     = null
}

variable "ipv6_subnet_azs" {
  description = "Availability zones to place the two IPv6-only ECS subnets in."
  type        = list(string)
  default     = ["us-east-1a", "us-east-1b"]
}

# --- ECR / container image -------------------------------------------------

variable "ecr_repository_name" {
  description = "Existing ECR repository holding the built backend image."
  type        = string
  default     = "production/adoptu"
}

variable "container_image_tag" {
  description = "Tag (or digest, e.g. 'sha256:...') of the image to deploy. Set via tfvars/CI on each release."
  type        = string
  default     = "latest"
}

variable "container_port" {
  description = "Port the Ktor app listens on inside the container (matches Dockerfile EXPOSE / application.conf ktor.deployment.port)."
  type        = number
  default     = 8080
}

# --- ECS Fargate sizing ------------------------------------------------------

variable "task_cpu" {
  type    = number
  default = 1024
}

variable "task_memory" {
  type    = number
  default = 1024
}

variable "desired_count" {
  description = "Number of running tasks. Keep at 1 unless the app is verified safe for concurrent instances (in-process state, scheduled jobs, etc.)."
  type        = number
  default     = 1
}

# --- RDS ---------------------------------------------------------------------

variable "db_identifier" {
  type    = string
  default = "adoptu"
}

variable "db_name" {
  type    = string
  default = "adoptu"
}

variable "db_username" {
  description = "RDS master username (the 'postgres' superuser, matches the live instance - immutable after creation)."
  type        = string
  default     = "postgres"
}

variable "db_app_password" {
  description = <<-EOT
    Password for the application-level 'adoptu' Postgres role (created via SQL inside
    the database, NOT by this RDS resource - the RDS master user is 'postgres', a
    separate superuser). Must equal the role's actual current password or the app
    will fail to connect. No default on purpose: set it in a gitignored tfvars file
    or pass via TF_VAR_db_app_password. To rotate, coordinate an `ALTER ROLE adoptu
    WITH PASSWORD '...'` against the database with updating this value and redeploying.
  EOT
  type        = string
  sensitive   = true
}

variable "db_kms_key_id" {
  description = "KMS key ARN for storage encryption. Set to the live instance's key (see README) to avoid a permanent plan diff after import; kms_key_id cannot change post-creation."
  type        = string
  default     = null
}

variable "db_instance_class" {
  type    = string
  default = "db.t4g.micro"
}

variable "db_engine_version" {
  type    = string
  default = "17.9"
}

variable "db_allocated_storage" {
  type    = number
  default = 20
}

variable "db_max_allocated_storage" {
  type    = number
  default = 1000
}

variable "db_deletion_protection" {
  description = "Live DB currently has this off; defaulting to true for the new config. Set to false only if you intend to destroy/recreate the DB."
  type        = bool
  default     = true
}

# --- App environment / admin -------------------------------------------------

variable "admin_email" {
  type    = string
  default = "adopt-u@adopt-u.org"
}

variable "admin_username" {
  type    = string
  default = "adopt-u@adopt-u.org"
}

variable "webauthn_origin" {
  type    = string
  default = "https://www.adopt-u.org"
}

variable "webauthn_rp_id" {
  type    = string
  default = "adopt-u.org"
}

# --- S3 image buckets ---------------------------------------------------------

variable "static_bucket_name" {
  type    = string
  default = "adoptu-static-images"
}

variable "dynamic_bucket_name" {
  type    = string
  default = "adoptu-dynamic-images"
}
