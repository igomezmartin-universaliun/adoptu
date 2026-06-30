# adopt-u AWS infrastructure (OpenTofu)

This codifies the **real, currently-running** adopt-u deployment in AWS
account `174000857825` (us-east-1), discovered by inspecting the live
account on 2026-06-30 — there was no prior IaC; everything was built by
hand via the AWS Console (the IAM role `ecsInfrastructureRoleForExpressServices`
and auto-named security groups indicate the ECS "Express" deploy wizard).

Domain: `adopt-u.org` (existing Route 53 zone `Z02544353GT8GHBAPPWSE`,
ACM cert already issued, both reused via data sources — not recreated).

**No load balancer.** CloudFront's app origin points directly at the
running ECS Fargate task over IPv6, matching the live deployment exactly
(verified against the live distribution's actual `CustomOriginConfig`:
`http-only`, port `8080`) — not an ALB/NLB. The address this needs to track
the task's current IPv6 address is `backend.adopt-u.org` (`route53.tf`) —
an **internal-only** hostname, never exposed to a browser, distinct from
`api.adopt-u.org` (which stays public and CloudFront-fronted, exactly as
today). `backend.adopt-u.org` is kept current **fully automatically** by
`dns_updater.tf`: an EventBridge rule fires on every "ECS task reached
RUNNING" event for the cluster, triggering a small Lambda that looks up
the new task's IPv6 address and UPSERTs the Route 53 record. No manual
step, ever — see "Releasing new versions" below.

**FIDO/WebAuthn**: `ADOPTU_WEB_AUTHN_ORIGIN` (`https://www.adopt-u.org`)
and `ADOPTU_WEB_AUTHN_RP_ID` (`adopt-u.org`) in `ecs.tf`'s container
environment are unchanged from the live task definition. `backend.adopt-u.org`
is pure server-side CDN-to-origin plumbing — it's never sent to or seen by
a browser, so it cannot affect WebAuthn relying-party/origin validation or
invalidate existing users' passkeys.

## What this is NOT

This is **not** a blank-slate `tofu apply`. Several resources already exist
with real data/identity behind them (S3 bucket names are globally unique,
the RDS instance holds real rows, CloudFront aliases must be globally
unique per account) — `apply`-ing this against an empty state will fail
with `BucketAlreadyExists` / `DBInstanceAlreadyExists` / `CNAMEAlreadyExists`
errors. Those resources must be **imported** first (commands below).

Compute (ECS cluster/service/task definition/security groups/the DNS
updater Lambda) is instead stood up **fresh, in parallel**, under a new
`adoptu` ECS cluster, so the existing live service (`Adopt-u-ipv6` in
cluster `default`) keeps serving traffic unmodified until you've verified
the new stack and are ready to cut over.

## Findings from the live account (fixed here, not silently)

1. **`ADOPTU_DB_PASSWORD` was a plaintext env var** in the ECS task
   definition (`Ad0ptU` — the same value documented as the *dev* default
   in `AGENTS.md`). Now read from Secrets Manager (`secrets.tf`).
2. **The task IAM policy's S3 statement was unscoped** — it referenced the
   literal placeholder `arn:aws:s3:::your-bucket-name`, not the real
   buckets. Fixed in `iam.tf`.
3. **Container port mismatch**: the live task definition maps port `80`,
   but the app actually listens on `8080` (`Dockerfile` `EXPOSE 8080`,
   `application.conf` `ktor.deployment.port = 8080`). Fixed.
4. **RDS is IPv4-only** (`NetworkType: IPV4`). IPv6-only ECS tasks have no
   IPv4 address at all, so they cannot reach an IPv4-only endpoint. Fixed
   by setting `network_type = "DUAL"` on the instance (its subnet group
   already supports `DUAL`).
5. The `adoptu-dynamic-images` bucket policy references **two** CloudFront
   distribution ARNs, one of which (`E1ASJX2YFBE5IN`) doesn't exist in the
   account anymore — a stale leftover. Dropped.
6. RDS `deletion_protection` was `false` and `backup_retention_period` was
   `1` day. Defaulted to `true` / `7` days here (override via
   `db_deletion_protection` if you actually need to destroy/recreate it).
7. There's also a stale Route 53 `AAAA` alias to an
   `ecs-express-gateway-alb` that no longer exists
   (`describe-load-balancers` returns nothing for the account) — leftover
   from an earlier, abandoned attempt at exactly the load-balanced design
   this stack deliberately does *not* use. Not imported/managed; delete it
   manually whenever convenient (it isn't referenced by anything).
8. The live deployment had a hand-maintained `ecs.adopt-u.org` record that
   required a manual update after every deploy. Replaced with
   `backend.adopt-u.org` + the `dns_updater` Lambda (still no load
   balancer, just no manual step either).

## Prerequisites

```bash
brew install opentofu   # or see https://opentofu.org/docs/intro/install
aws configure --profile adoptu   # already set up; account 174000857825
```

Set the one required secret (the **current** password of the `adoptu`
Postgres role — find it whoever has DB access; it is not retrievable from
AWS):

```bash
export TF_VAR_db_app_password='<current adoptu role password>'
```

## Step 1 — import the resources that already exist

```bash
cd infra
tofu init

tofu import aws_s3_bucket.static_images adoptu-static-images
tofu import aws_s3_bucket.dynamic_images adoptu-dynamic-images
tofu import aws_s3_bucket_public_access_block.static_images adoptu-static-images
tofu import aws_s3_bucket_public_access_block.dynamic_images adoptu-dynamic-images
tofu import aws_s3_bucket_policy.static_images adoptu-static-images
tofu import aws_s3_bucket_policy.dynamic_images adoptu-dynamic-images

tofu import aws_db_instance.postgres adoptu

tofu import aws_cloudfront_distribution.static_images E2RUPHX2ZLDSED
tofu import aws_cloudfront_distribution.dynamic_images E2K4MKM9AZ21BM
tofu import aws_cloudfront_distribution.app E1NABP27JA8QPA

tofu import aws_iam_role.ecs_task adoptuAppServicesPerms
tofu import aws_iam_policy.ecs_task_app arn:aws:iam::174000857825:policy/adoptuAppServicesPermsPolicy
tofu import aws_iam_role_policy_attachment.ecs_task_app \
  "adoptuAppServicesPerms/arn:aws:iam::174000857825:policy/adoptuAppServicesPermsPolicy"

# Existing DNS records (most names already exist in the zone; importing
# avoids "RRSet already exists" on apply). <ZONE_ID> = Z02544353GT8GHBAPPWSE
# Note: backend.adopt-u.org is NOT imported here - it's a brand new name,
# nothing to import.
tofu import 'aws_route53_record.app_a["adopt-u.org"]' Z02544353GT8GHBAPPWSE_adopt-u.org_A
tofu import 'aws_route53_record.app_aaaa["adopt-u.org"]' Z02544353GT8GHBAPPWSE_adopt-u.org_AAAA
tofu import 'aws_route53_record.app_a["www.adopt-u.org"]' Z02544353GT8GHBAPPWSE_www.adopt-u.org_A
tofu import 'aws_route53_record.app_a["api.adopt-u.org"]' Z02544353GT8GHBAPPWSE_api.adopt-u.org_A
tofu import 'aws_route53_record.app_aaaa["api.adopt-u.org"]' Z02544353GT8GHBAPPWSE_api.adopt-u.org_AAAA
tofu import aws_route53_record.static_a Z02544353GT8GHBAPPWSE_static.adopt-u.org_A
tofu import aws_route53_record.dynamic_a Z02544353GT8GHBAPPWSE_dynamic.adopt-u.org_A
```

Then:

```bash
tofu plan
```

Read the plan carefully. Expect diffs for the deliberate fixes above (S3
policy ARNs, IAM policy resources, RDS `network_type`/security
group/`deletion_protection`). You should **not** see anything proposing to
destroy/recreate the S3 buckets, the RDS instance, or any CloudFront
distribution — if you do, stop and figure out why before applying (likely
an import ID mismatch).

## Step 2 — stand up the new compute (no impact on the live service)

The DNS updater (Lambda + EventBridge rule) must exist **before** the new
ECS service starts its first task, or that first task's `RUNNING` event
fires into the void and `backend.adopt-u.org` is stuck on its seed value
until the next deploy:

```bash
tofu apply -target=aws_iam_role.dns_updater \
           -target=aws_iam_role_policy.dns_updater \
           -target=aws_cloudwatch_log_group.dns_updater \
           -target=aws_lambda_function.dns_updater \
           -target=aws_cloudwatch_event_rule.ecs_task_running \
           -target=aws_cloudwatch_event_target.dns_updater \
           -target=aws_lambda_permission.events_invoke_dns_updater \
           -target=aws_route53_record.backend

tofu apply -target=aws_subnet.ecs_ipv6_only \
           -target=aws_security_group.ecs_task \
           -target=aws_security_group.rds \
           -target=aws_ecs_cluster.this \
           -target=aws_iam_role.ecs_execution \
           -target=aws_ecs_task_definition.app \
           -target=aws_ecs_service.app
```

Confirm the Lambda actually updated the record (CloudWatch Logs
`/aws/lambda/adoptu-dns-updater` should show an `"updated"` result), then
verify the task directly (bypassing DNS/CloudFront, in case the record
hasn't propagated to your resolver yet):

```bash
dig +short AAAA backend.adopt-u.org
curl -g -v "http://[$(dig +short AAAA backend.adopt-u.org)]:8080/"
```

## Step 3 — cut over

```bash
tofu apply
```

This applies everything else (RDS `network_type`/SG, the imported Route53
records, the CloudFront app origin pointing at `backend.adopt-u.org`).
CloudFront resolves `backend.adopt-u.org` per-request at its DNS TTL (60s),
so this is the moment apex/www/api traffic moves to the new task. Watch
error rates for a few minutes before proceeding.

## Step 4 — decommission the old manual resources

After confirming the new stack is serving correctly for a while:

```bash
aws ecs update-service --cluster default --service Adopt-u-ipv6 --desired-count 0 --profile adoptu
aws ecs delete-service --cluster default --service Adopt-u-ipv6 --profile adoptu

# Old hand-maintained record, no longer referenced by anything:
# aws route53 change-resource-record-sets --hosted-zone-id Z02544353GT8GHBAPPWSE --profile adoptu \
#   --change-batch '{"Changes":[{"Action":"DELETE","ResourceRecordSet":{"Name":"ecs.adopt-u.org.","Type":"AAAA", ...}}]}'
# (check its current value with list-resource-record-sets first)

# Stale orphaned alias from an abandoned ALB attempt - not referenced by
# anything; check its current value with list-resource-record-sets first.
# aws route53 change-resource-record-sets --hosted-zone-id Z02544353GT8GHBAPPWSE --profile adoptu \
#   --change-batch '{"Changes":[{"Action":"DELETE","ResourceRecordSet":{...}}]}'
```

## Releasing new versions

`buildspec.yml` builds and pushes to ECR but was never wired into a real
CodeBuild/CodePipeline project (both are empty in the account) — releases
have been manual `docker build` + `push` + Console redeploys.

To ship a new image with this stack:

```bash
tofu apply -var="container_image_tag=sha256:<new digest>"
```

That's it — every deploy replaces the task (new IP), and the
`dns_updater` Lambda picks up the new task's `RUNNING` event automatically
and re-points `backend.adopt-u.org`. No manual DNS step.

## Things intentionally left unmanaged

- **SES** (`ses.tf`): domain identity already verified, DKIM/SPF/DMARC
  already published. Re-declaring it risks Terraform "fixing" already-correct
  verification records.
- **Mail records** (MX, SPF, DKIM CNAMEs, DMARC, autoconfig/autodiscover):
  Hostinger-hosted mail, unrelated to this stack.
- **NS / SOA**: zone infrastructure records, never touch these.
