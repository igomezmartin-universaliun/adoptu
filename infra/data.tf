# Pre-existing resources this stack plugs into. These are read-only lookups;
# none of them are created or destroyed by this config.

data "aws_caller_identity" "current" {}

data "aws_vpc" "target" {
  id      = var.vpc_id
  default = var.vpc_id == null ? true : null
}

data "aws_route53_zone" "primary" {
  name         = "${var.domain_name}."
  private_zone = false
}

# Wildcard cert (*.adopt-u.org + adopt-u.org) already issued and validated;
# used by both CloudFront (us-east-1) and the regional ALB listener.
data "aws_acm_certificate" "wildcard_us_east_1" {
  provider    = aws.us_east_1
  domain      = "*.${var.domain_name}"
  statuses    = ["ISSUED"]
  most_recent = true
}

data "aws_acm_certificate" "wildcard_regional" {
  domain      = "*.${var.domain_name}"
  statuses    = ["ISSUED"]
  most_recent = true
}

data "aws_ecr_repository" "backend" {
  name = var.ecr_repository_name
}

data "aws_internet_gateway" "main" {
  filter {
    name   = "attachment.vpc-id"
    values = [data.aws_vpc.target.id]
  }
}

# Auto-created by the RDS console for the default VPC; reused as-is rather
# than re-creating, since the live "adoptu" instance already sits in it.
data "aws_db_subnet_group" "default" {
  name = "default-${data.aws_vpc.target.id}"
}

data "aws_iam_role" "rds_monitoring" {
  name = "rds-monitoring-role"
}

# Default dual-stack public subnets (have both an IPv4 CIDR and an
# Amazon-provided IPv6 CIDR) - the ALB needs to live here rather than in
# the IPv6-only subnets, since ALB nodes require a subnet with an IPv4
# CIDR present even when not allocating a public IPv4 address (see alb.tf).
data "aws_subnet" "alb" {
  for_each = toset(var.ipv6_subnet_azs)

  vpc_id            = data.aws_vpc.target.id
  availability_zone = each.key

  filter {
    name   = "default-for-az"
    values = ["true"]
  }
}
