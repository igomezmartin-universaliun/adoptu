# SES is intentionally not managed here. The domain identity
# "adopt-u.org" is already verified (DKIM CNAMEs, SPF and DMARC TXT
# records already published in the zone - see infra/README.md), and the
# app sends via the SES API directly (SesEmailAdapter using AWS_REGION /
# AWS_SES_ENDPOINT, see backend/src/main/kotlin/com/adoptu/adapters/
# notification/SesEmailAdapter.kt), not SMTP, so no SMTP IAM user or
# credentials are needed either. The buildspec.yml SMTP_* secrets are
# unused leftovers from an earlier design.
#
# Re-declaring the identity as a managed resource would risk Terraform
# trying to "fix" already-correct verification records. The only thing
# this stack adds is the task IAM role's ses:SendEmail permission, in
# iam.tf, scoped the same way the live (buggy) policy intended.
