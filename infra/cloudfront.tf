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

# --- adopt-u.org / www / api: the app itself, fronting the ALB -------------
# Live setup pointed this at a hand-maintained DNS record holding one
# Fargate task's IP directly (broke on every task restart). Points at the
# ALB's stable DNS name instead.

resource "aws_cloudfront_distribution" "app" {
  enabled         = true
  is_ipv6_enabled = true
  price_class     = "PriceClass_All"
  http_version    = "http2"
  aliases         = [var.domain_name, "www.${var.domain_name}", "api.${var.domain_name}"]
  comment         = "adopt-u app (ECS Fargate via ALB)"

  origin {
    domain_name = aws_lb.app.dns_name
    origin_id   = "app-alb"

    custom_origin_config {
      http_port              = 80
      https_port             = 443
      origin_protocol_policy = "https-only"
      origin_ssl_protocols   = ["TLSv1.2"]
    }
  }

  default_cache_behavior {
    target_origin_id         = "app-alb"
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
