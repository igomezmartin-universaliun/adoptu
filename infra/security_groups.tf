resource "aws_security_group" "alb" {
  name        = "adoptu-alb-sg"
  description = "Public ALB for adopt-u.org - HTTP/HTTPS over IPv4 and IPv6"
  vpc_id      = data.aws_vpc.target.id

  ingress {
    description      = "HTTPS"
    from_port        = 443
    to_port          = 443
    protocol         = "tcp"
    cidr_blocks      = ["0.0.0.0/0"]
    ipv6_cidr_blocks = ["::/0"]
  }

  ingress {
    description      = "HTTP (redirected to HTTPS by the listener)"
    from_port        = 80
    to_port          = 80
    protocol         = "tcp"
    cidr_blocks      = ["0.0.0.0/0"]
    ipv6_cidr_blocks = ["::/0"]
  }

  egress {
    from_port        = 0
    to_port          = 0
    protocol         = "-1"
    cidr_blocks      = ["0.0.0.0/0"]
    ipv6_cidr_blocks = ["::/0"]
  }

  tags = {
    Name = "adoptu-alb-sg"
  }
}

resource "aws_security_group" "ecs_task" {
  name        = "adoptu-ecs-task-sg"
  description = "Adopt-u Fargate tasks - only reachable from the ALB"
  vpc_id      = data.aws_vpc.target.id

  ingress {
    description     = "App port from ALB only"
    from_port       = var.container_port
    to_port         = var.container_port
    protocol        = "tcp"
    security_groups = [aws_security_group.alb.id]
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
