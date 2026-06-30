terraform {
  required_version = ">= 1.7.0"

  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.60"
    }
  }

  # State is local by default so this can be applied without any extra setup.
  # Before the first real apply, create a private S3 bucket + DynamoDB lock
  # table and switch to a remote backend, e.g.:
  #
  # backend "s3" {
  #   bucket         = "adoptu-tofu-state"
  #   key            = "adoptu/terraform.tfstate"
  #   region         = "us-east-1"
  #   dynamodb_table = "adoptu-tofu-locks"
  #   encrypt        = true
  # }
}
