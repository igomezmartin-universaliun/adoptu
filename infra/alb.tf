# Public ALB in front of the ECS service. Lives in the default dual-stack
# subnets (ALB nodes require a subnet with an IPv4 CIDR present) but uses
# "dualstack-without-public-ipv4" so the load balancer itself never
# allocates or uses a public IPv4 address - it's reachable over IPv6 only,
# same as the ECS tasks behind it. CloudFront (which supports both
# protocols) is still the public entry point for end users; the ALB only
# needs to be reachable from CloudFront's IPv6 ranges and from IPv6
# health-checkers, which dualstack-without-public-ipv4 satisfies.

resource "aws_lb" "app" {
  name               = "adoptu-alb"
  internal           = false
  load_balancer_type = "application"
  ip_address_type    = "dualstack-without-public-ipv4"

  subnets         = [for s in data.aws_subnet.alb : s.id]
  security_groups = [aws_security_group.alb.id]

  enable_deletion_protection = true

  tags = {
    Name = "adoptu-alb"
  }
}

resource "aws_lb_target_group" "app" {
  name            = "adoptu-app"
  port            = var.container_port
  protocol        = "HTTP"
  vpc_id          = data.aws_vpc.target.id
  target_type     = "ip"
  ip_address_type = "ipv6"

  health_check {
    path                = "/"
    matcher             = "200-399"
    interval            = 30
    timeout             = 5
    healthy_threshold   = 2
    unhealthy_threshold = 3
  }

  tags = {
    Name = "adoptu-app"
  }
}

resource "aws_lb_listener" "https" {
  load_balancer_arn = aws_lb.app.arn
  port              = 443
  protocol          = "HTTPS"
  ssl_policy        = "ELBSecurityPolicy-TLS13-1-2-2021-06"
  certificate_arn   = data.aws_acm_certificate.wildcard_regional.arn

  default_action {
    type             = "forward"
    target_group_arn = aws_lb_target_group.app.arn
  }
}

resource "aws_lb_listener" "http_redirect" {
  load_balancer_arn = aws_lb.app.arn
  port              = 80
  protocol          = "HTTP"

  default_action {
    type = "redirect"

    redirect {
      port        = "443"
      protocol    = "HTTPS"
      status_code = "HTTP_301"
    }
  }
}
