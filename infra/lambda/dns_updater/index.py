import os

import boto3

ecs = boto3.client("ecs")
ec2 = boto3.client("ec2")
route53 = boto3.client("route53")


def handler(event, context):
    detail = event.get("detail", {})
    if detail.get("lastStatus") != "RUNNING":
        return {"skipped": f"lastStatus={detail.get('lastStatus')}"}

    cluster_arn = detail["clusterArn"]
    task_arn = detail["taskArn"]

    tasks = ecs.describe_tasks(cluster=cluster_arn, tasks=[task_arn])["tasks"]
    if not tasks:
        return {"skipped": "task not found (already stopped?)"}

    eni_id = next(
        (
            d["value"]
            for attachment in tasks[0].get("attachments", [])
            if attachment.get("type") == "ElasticNetworkInterface"
            for d in attachment.get("details", [])
            if d["name"] == "networkInterfaceId"
        ),
        None,
    )
    if eni_id is None:
        return {"skipped": "task has no ENI attachment"}

    eni = ec2.describe_network_interfaces(NetworkInterfaceIds=[eni_id])["NetworkInterfaces"][0]
    ipv6_addresses = eni.get("Ipv6Addresses", [])
    if not ipv6_addresses:
        return {"skipped": "ENI has no IPv6 address yet"}
    ipv6 = ipv6_addresses[0]["Ipv6Address"]

    record_name = os.environ["RECORD_NAME"]
    route53.change_resource_record_sets(
        HostedZoneId=os.environ["HOSTED_ZONE_ID"],
        ChangeBatch={
            "Changes": [
                {
                    "Action": "UPSERT",
                    "ResourceRecordSet": {
                        "Name": record_name,
                        "Type": "AAAA",
                        "TTL": 60,
                        "ResourceRecords": [{"Value": ipv6}],
                    },
                }
            ]
        },
    )
    return {"updated": record_name, "value": ipv6}
