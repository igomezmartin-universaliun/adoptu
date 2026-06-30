resource "aws_security_group" "ecs_task" {
  name        = "adoptu-ecs-task-sg"
  description = "Adopt-u Fargate tasks - CloudFront origins straight to the task over IPv6, no load balancer"
  vpc_id      = data.aws_vpc.target.id

  # No managed prefix list for CloudFront's IPv6 origin-facing ranges exists
  # (only an IPv4 one does), so this is wide open on IPv6 - same as the
  # live deployment's existing security group for this exact reason.
  ingress {
    description      = "App port from CloudFront (direct origin, no LB)"
    from_port        = var.container_port
    to_port          = var.container_port
    protocol         = "tcp"
    ipv6_cidr_blocks = ["::/0"]
  }

  egress {
    description      = "ECR pulls, CloudWatch Logs, Secrets Manager, SES, S3 - all over IPv6 via IGW, no NAT"
    from_port        = 0
    to_port          = 0
    protocol         = "-1"
    cidr_blocks      = ["0.0.0.0/0"]
    ipv6_cidr_blocks = ["::/0"]
  }

  tags = {
    Name = "adoptu-ecs-task-sg"
  }
}

resource "aws_security_group" "rds" {
  name        = "adoptu-rds-sg"
  description = "Adopt-u Postgres - only reachable from ECS tasks"
  vpc_id      = data.aws_vpc.target.id

  ingress {
    description     = "Postgres from ECS tasks only"
    from_port       = 5432
    to_port         = 5432
    protocol        = "tcp"
    security_groups = [aws_security_group.ecs_task.id]
  }

  egress {
    from_port        = 0
    to_port          = 0
    protocol         = "-1"
    cidr_blocks      = ["0.0.0.0/0"]
    ipv6_cidr_blocks = ["::/0"]
  }

  tags = {
    Name = "adoptu-rds-sg"
  }
}
