data "aws_cloudfront_cache_policy" "caching_optimized" {
  name = "Managed-CachingOptimized"
}

data "aws_cloudfront_cache_policy" "caching_disabled" {
  name = "Managed-CachingDisabled"
}

data "aws_cloudfront_origin_request_policy" "all_viewer" {
  name = "Managed-AllViewer"
}

resource "aws_cloudfront_origin_access_control" "s3" {
  name                              = "adoptu-s3-oac"
  origin_access_control_origin_type = "s3"
  signing_behavior                  = "always"
  signing_protocol                  = "sigv4"
}

# --- static.adopt-u.org: long-lived static assets --------------------------

resource "aws_cloudfront_distribution" "static_images" {
  enabled         = true
  is_ipv6_enabled = true
  price_class     = "PriceClass_All"
  http_version    = "http2"
  aliases         = ["static.${var.domain_name}"]
  comment         = "adopt-u static images"

  origin {
    domain_name              = aws_s3_bucket.static_images.bucket_regional_domain_name
    origin_id                = "static-images-s3"
    origin_access_control_id = aws_cloudfront_origin_access_control.s3.id
  }

  default_cache_behavior {
    target_origin_id       = "static-images-s3"
    viewer_protocol_policy = "redirect-to-https"
    allowed_methods        = ["GET", "HEAD"]
    cached_methods         = ["GET", "HEAD"]
    compress               = true
    cache_policy_id        = data.aws_cloudfront_cache_policy.caching_optimized.id
  }

  restrictions {
    geo_restriction {
      restriction_type = "none"
    }
  }

  viewer_certificate {
    acm_certificate_arn      = data.aws_acm_certificate.wildcard_us_east_1.arn
    ssl_support_method       = "sni-only"
    minimum_protocol_version = "TLSv1.2_2021"
  }
}

# --- dynamic.adopt-u.org: user-uploaded pet images ---------------------------

resource "aws_cloudfront_distribution" "dynamic_images" {
  enabled         = true
  is_ipv6_enabled = true
  price_class     = "PriceClass_All"
  http_version    = "http2"
  aliases         = ["dynamic.${var.domain_name}"]
  comment         = "adopt-u dynamic (uploaded) images"

  origin {
    domain_name              = aws_s3_bucket.dynamic_images.bucket_regional_domain_name
    origin_id                = "dynamic-images-s3"
    origin_access_control_id = aws_cloudfront_origin_access_control.s3.id
  }

  default_cache_behavior {
    target_origin_id       = "dynamic-images-s3"
    viewer_protocol_policy = "redirect-to-https"
    allowed_methods        = ["GET", "HEAD"]
    cached_methods         = ["GET", "HEAD"]
    compress               = true
    cache_policy_id        = data.aws_cloudfront_cache_policy.caching_optimized.id
  }

  restrictions {
    geo_restriction {
      restriction_type = "none"
    }
  }

  viewer_certificate {
    acm_certificate_arn      = data.aws_acm_certificate.wildcard_us_east_1.arn
    ssl_support_method       = "sni-only"
    minimum_protocol_version = "TLSv1.2_2021"
  }
}

# --- adopt-u.org / www / api: the app itself, fronting the ECS task directly -
# No load balancer, by design - same as the live deployment. Origin is the
# "ecs.<domain>" DNS record (route53.tf), which resolves straight to the
# running Fargate task's public IPv6 address. That record is NOT
# auto-updated by this stack (see route53.tf / README "Releasing new
# versions") - re-point it after every deploy that replaces the task,
# exactly like today.

resource "aws_cloudfront_distribution" "app" {
  enabled         = true
  is_ipv6_enabled = true
  price_class     = "PriceClass_All"
  http_version    = "http2"
  aliases         = [var.domain_name, "www.${var.domain_name}", "api.${var.domain_name}"]
  comment         = "adopt-u app (ECS Fargate, direct origin, no load balancer)"

  origin {
    domain_name = "ecs.${var.domain_name}"
    origin_id   = "ecs-task"

    custom_origin_config {
      http_port              = var.container_port
      https_port             = 443 # unused (origin_protocol_policy is http-only - matches live, no TLS to origin)
      origin_protocol_policy = "http-only"
      origin_ssl_protocols   = ["TLSv1", "TLSv1.1", "TLSv1.2"] # unused under http-only; matches live config exactly for clean import
    }
  }

  default_cache_behavior {
    target_origin_id         = "ecs-task"
    viewer_protocol_policy   = "redirect-to-https"
    allowed_methods          = ["GET", "HEAD", "OPTIONS", "PUT", "POST", "PATCH", "DELETE"]
    cached_methods           = ["GET", "HEAD"]
    compress                 = true
    cache_policy_id          = data.aws_cloudfront_cache_policy.caching_disabled.id
    origin_request_policy_id = data.aws_cloudfront_origin_request_policy.all_viewer.id
  }

  restrictions {
    geo_restriction {
      restriction_type = "none"
    }
  }

  viewer_certificate {
    acm_certificate_arn      = data.aws_acm_certificate.wildcard_us_east_1.arn
    ssl_support_method       = "sni-only"
    minimum_protocol_version = "TLSv1.2_2021"
  }
}
