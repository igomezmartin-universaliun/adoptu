# Two IPv6-only subnets for ECS Fargate tasks. No NAT Gateway, no
# Egress-Only Internet Gateway: the VPC's IPv6 allocation is "public"
# (Amazon-provided /56), so the main route table's `::/0 -> igw` route
# gives these subnets direct internet egress over IPv6 only. IPv4-only
# resources (the ALB, the default dual-stack subnets) are untouched.
#
# Deliberately not creating/associating an explicit route table: the
# account's default VPC main route table already has the ::/0 -> igw
# route (and the IPv4 equivalent for the existing dual-stack subnets),
# and any subnet without an explicit association inherits it automatically.
# Managing the main route table here would risk touching every other
# subnet in the default VPC, which is out of scope for this stack.

resource "aws_subnet" "ecs_ipv6_only" {
  for_each = { for idx, az in var.ipv6_subnet_azs : az => idx }

  vpc_id            = data.aws_vpc.target.id
  availability_zone = each.key

  ipv6_native     = true
  ipv6_cidr_block = cidrsubnet(data.aws_vpc.target.ipv6_cidr_block, 8, 6 + each.value)
  enable_dns64    = false

  assign_ipv6_address_on_creation                = true
  private_dns_hostname_type_on_launch            = "resource-name"
  enable_resource_name_dns_aaaa_record_on_launch = true

  tags = {
    Name = "adoptu-ecs-ipv6-only-${each.key}"
  }
}
