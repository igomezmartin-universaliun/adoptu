output "alb_dns_name" {
  value = aws_lb.app.dns_name
}

output "ecs_cluster_name" {
  value = aws_ecs_cluster.this.name
}

output "rds_endpoint" {
  value = aws_db_instance.postgres.address
}

output "cloudfront_app_domain" {
  value = aws_cloudfront_distribution.app.domain_name
}

output "cloudfront_static_domain" {
  value = aws_cloudfront_distribution.static_images.domain_name
}

output "cloudfront_dynamic_domain" {
  value = aws_cloudfront_distribution.dynamic_images.domain_name
}

output "ecs_ipv6_subnet_ids" {
  value = { for az, s in aws_subnet.ecs_ipv6_only : az => s.id }
}
