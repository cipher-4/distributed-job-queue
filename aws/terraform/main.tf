terraform {
  required_version = ">= 1.5.0"
  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.0"
    }
  }
}

provider "aws" {
  region = var.aws_region
}

variable "aws_region" {
  default = "us-east-1"
}

# 1. AWS RDS PostgreSQL 16 Instance
resource "aws_db_instance" "job_queue_db" {
  identifier          = "reliable-job-queue-db"
  allocated_storage   = 20
  engine              = "postgres"
  engine_version      = "16.1"
  instance_class      = "db.t4g.micro"
  db_name             = "jobqueuedb"
  username            = "postgres"
  password            = var.db_password
  skip_final_snapshot = true
}

variable "db_password" {
  type      = string
  sensitive = true
}

# 2. AWS ECS Cluster
resource "aws_ecs_cluster" "queue_cluster" {
  name = "reliable-job-queue-cluster"
}

# 3. CloudWatch Log Groups
resource "aws_cloudwatch_log_group" "producer_logs" {
  name              = "/ecs/reliable-job-queue-producer"
  retention_in_days = 7
}

resource "aws_cloudwatch_log_group" "worker_logs" {
  name              = "/ecs/reliable-job-queue-worker"
  retention_in_days = 7
}

output "rds_endpoint" {
  value = aws_db_instance.job_queue_db.endpoint
}

output "ecs_cluster_name" {
  value = aws_ecs_cluster.queue_cluster.name
}
