# Only the records tied to resources this stack manages. The zone's other
# ~20 records (mail MX/SPF/DKIM/DMARC for Hostinger, SES verification
# TXT/DKIM, NS/SOA, the ACM validation CNAME) are already correct and
# deliberately left untouched here - see infra/README.md.

locals {
  app_hostnames = [var.domain_name, "www.${var.domain_name}", "api.${var.domain_name}"]
}

# Points at whichever Fargate task is currently running - the CloudFront
# "app" origin (cloudfront.tf) targets this name rather than a load
# balancer. Brought under management via `tofu import` (it already exists
# live); `ignore_changes` because its value is updated operationally after
# each deploy that replaces the task (see README "Releasing new
# versions"), not by Terraform.
resource "aws_route53_record" "ecs_task" {
  zone_id = data.aws_route53_zone.primary.zone_id
  name    = "ecs.${var.domain_name}"
  type    = "AAAA"
  ttl     = 60
  records = ["2600:1f18:4f77:4e01:386a:77e7:3b9e:7605"] # seeded from the current live task; updated operationally after that

  lifecycle {
    ignore_changes = [records]
  }
}

resource "aws_route53_record" "app_a" {
  for_each = toset(local.app_hostnames)

  zone_id = data.aws_route53_zone.primary.zone_id
  name    = each.key
  type    = "A"

  alias {
    name                   = aws_cloudfront_distribution.app.domain_name
    zone_id                = aws_cloudfront_distribution.app.hosted_zone_id
    evaluate_target_health = false
  }
}

resource "aws_route53_record" "app_aaaa" {
  for_each = toset(local.app_hostnames)

  zone_id = data.aws_route53_zone.primary.zone_id
  name    = each.key
  type    = "AAAA"

  alias {
    name                   = aws_cloudfront_distribution.app.domain_name
    zone_id                = aws_cloudfront_distribution.app.hosted_zone_id
    evaluate_target_health = false
  }
}

resource "aws_route53_record" "static_a" {
  zone_id = data.aws_route53_zone.primary.zone_id
  name    = "static.${var.domain_name}"
  type    = "A"

  alias {
    name                   = aws_cloudfront_distribution.static_images.domain_name
    zone_id                = aws_cloudfront_distribution.static_images.hosted_zone_id
    evaluate_target_health = false
  }
}

resource "aws_route53_record" "static_aaaa" {
  zone_id = data.aws_route53_zone.primary.zone_id
  name    = "static.${var.domain_name}"
  type    = "AAAA"

  alias {
    name                   = aws_cloudfront_distribution.static_images.domain_name
    zone_id                = aws_cloudfront_distribution.static_images.hosted_zone_id
    evaluate_target_health = false
  }
}

resource "aws_route53_record" "dynamic_a" {
  zone_id = data.aws_route53_zone.primary.zone_id
  name    = "dynamic.${var.domain_name}"
  type    = "A"

  alias {
    name                   = aws_cloudfront_distribution.dynamic_images.domain_name
    zone_id                = aws_cloudfront_distribution.dynamic_images.hosted_zone_id
    evaluate_target_health = false
  }
}

resource "aws_route53_record" "dynamic_aaaa" {
  zone_id = data.aws_route53_zone.primary.zone_id
  name    = "dynamic.${var.domain_name}"
  type    = "AAAA"

  alias {
    name                   = aws_cloudfront_distribution.dynamic_images.domain_name
    zone_id                = aws_cloudfront_distribution.dynamic_images.hosted_zone_id
    evaluate_target_health = false
  }
}
