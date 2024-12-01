import json
import boto3
import logging

# Set up logging
logger = logging.getLogger()
logger.setLevel(logging.INFO)

# Initialize Bedrock client for the correct region
bedrock_runtime = boto3.client("bedrock-runtime", region_name="us-east-2")

def lambda_handler(event, context):
    try:
        # Parse the incoming request body to extract the query (prompt)
        body = json.loads(event["body"])
        prompt = body.get("query", "Default query: what does cats blinking slowly mean?")  # Fallback query if not provided

        # Define the request payload for the Bedrock model
        kwargs = {
            "modelId": "us.anthropic.claude-3-5-sonnet-20240620-v1:0",  # Update if needed
            "contentType": "application/json",
            "accept": "*/*",
            "body": json.dumps({
                "anthropic_version": "bedrock-2023-05-31",
                "max_tokens": 100,
                "messages": [
                    {
                        "role": "user",
                        "content": [
                            {"type": "text", "text": prompt}
                        ]
                    }
                ]
            }).encode("utf-8")
        }

        # Call Bedrock model
        resp = bedrock_runtime.invoke_model(**kwargs)

        # Parse and log the response
        resp_json = json.loads(resp.get("body").read())
        logger.info("Bedrock Response: %s", resp_json)

        return {
            "statusCode": 200,
            "body": json.dumps(resp_json)
        }
    except Exception as e:
        logger.error("Error invoking Bedrock model: %s", str(e))
        return {
            "statusCode": 500,
            "body": f"Error: {str(e)}"
        }
