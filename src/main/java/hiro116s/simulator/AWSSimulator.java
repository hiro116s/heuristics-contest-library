package hiro116s.simulator;

import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.AmazonEC2ClientBuilder;
import com.amazonaws.services.ec2.model.DescribeInstanceStatusRequest;
import com.amazonaws.services.ec2.model.DescribeInstancesRequest;
import com.amazonaws.services.ec2.model.DescribeSpotInstanceRequestsRequest;
import com.amazonaws.services.ec2.model.GroupIdentifier;
import com.amazonaws.services.ec2.model.IamInstanceProfileSpecification;
import com.amazonaws.services.ec2.model.InstanceType;
import com.amazonaws.services.ec2.model.LaunchSpecification;
import com.amazonaws.services.ec2.model.RequestSpotInstancesRequest;
import com.amazonaws.services.ec2.model.RequestSpotInstancesResult;
import com.amazonaws.services.ec2.model.SpotInstanceRequest;
import com.amazonaws.services.ec2.model.TerminateInstancesRequest;
import com.amazonaws.services.simplesystemsmanagement.AWSSimpleSystemsManagement;
import com.amazonaws.services.simplesystemsmanagement.AWSSimpleSystemsManagementClientBuilder;
import com.amazonaws.services.simplesystemsmanagement.model.AWSSimpleSystemsManagementException;
import com.amazonaws.services.simplesystemsmanagement.model.GetCommandInvocationRequest;
import com.amazonaws.services.simplesystemsmanagement.model.SendCommandRequest;
import com.amazonaws.services.simplesystemsmanagement.model.SendCommandResult;
import com.amazonaws.waiters.WaiterParameters;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;
import org.springframework.retry.support.RetryTemplate;

import java.util.List;
import java.util.stream.Collectors;

public class AWSSimulator {
    private final Arguments arguments;
    private final AmazonEC2 amazonEC2;
    private final AWSSimpleSystemsManagement amazonSSM;

    public AWSSimulator(Arguments arguments, AmazonEC2 amazonEC2, AWSSimpleSystemsManagement amazonSSM) {
        this.arguments = arguments;
        this.amazonEC2 = amazonEC2;
        this.amazonSSM = amazonSSM;
    }

    public static void main(String[] args) {
        final Arguments arguments = parseArgs(args);
        final AmazonEC2 ec2 = AmazonEC2ClientBuilder.defaultClient();
        final AWSSimpleSystemsManagement ssm = AWSSimpleSystemsManagementClientBuilder.defaultClient();
        new AWSSimulator(arguments, ec2, ssm).run();
    }

    private void run() {
        final RequestSpotInstancesRequest spotInstancesRequest = new RequestSpotInstancesRequest()
                .withLaunchSpecification(new LaunchSpecification()
                        .withImageId(arguments.imageId)
                        .withInstanceType(arguments.getInstanceType())
                        .withKeyName(arguments.keyName)
                        .withAllSecurityGroups(ImmutableList.of(new GroupIdentifier().withGroupId(arguments.securityGroup)))
                        .withIamInstanceProfile(new IamInstanceProfileSpecification().withArn(arguments.iamInstanceProfile)));
        final RequestSpotInstancesResult spotInstancesResult = amazonEC2.requestSpotInstances(spotInstancesRequest);
        System.err.println("spot instance requests are sent successfully. " + spotInstancesResult.getSpotInstanceRequests());

        final DescribeSpotInstanceRequestsRequest describeRequest = buildDescribeSpotInstanceRequest(spotInstancesResult);
        amazonEC2.waiters().spotInstanceRequestFulfilled().run(new WaiterParameters<>(describeRequest));
        final List<String> spotInstanceIds = amazonEC2.describeSpotInstanceRequests(describeRequest).getSpotInstanceRequests().stream()
                .map(SpotInstanceRequest::getInstanceId)
                .collect(Collectors.toList());
        System.err.println("spot instance requests are fulfilled (spotInstanceIds:" + spotInstanceIds + ")");
        try {
            amazonEC2.waiters().instanceRunning().run(new WaiterParameters<>(new DescribeInstancesRequest().withInstanceIds(spotInstanceIds)));
            amazonEC2.waiters().instanceStatusOk().run(new WaiterParameters<>(new DescribeInstanceStatusRequest().withInstanceIds(spotInstanceIds)));
            System.err.println("Instances started successfully");

            final SendCommandRequest sendCommandRequest = new SendCommandRequest()
                    .withDocumentName("AWS-RunRemoteScript")
                    .withInstanceIds(spotInstanceIds)
                    .withParameters(ImmutableMap.of(
                            "sourceType", ImmutableList.of("S3"),
                            "sourceInfo", ImmutableList.of(String.format("{\"path\":\"%s\"}", arguments.scriptPathInS3)),
                            "commandLine", ImmutableList.of(arguments.getCommandLine())));
            final SendCommandResult sendCommandResult = amazonSSM.sendCommand(sendCommandRequest);
            System.err.println("Command is sent successfully: " + sendCommandResult.getCommand().getCommandId());
            for (String spotInstanceId : spotInstanceIds) {
                final RetryTemplate commandExecutedWaiterRetry = RetryTemplate.builder()
                        .maxAttempts(5)
                        .fixedBackoff(3000)
                        .retryOn(AWSSimpleSystemsManagementException.class)
                        .build();
                commandExecutedWaiterRetry.execute(
                        ctx -> {
                            amazonSSM.waiters().commandExecuted().run(new WaiterParameters<>(new GetCommandInvocationRequest()
                                    .withInstanceId(spotInstanceId)
                                    .withCommandId(sendCommandResult.getCommand().getCommandId())
                                    .withPluginName("runShellScript")));
                            return null;
                        },
                        ctx -> {
                            System.err.println("Failed to run commandExecuted.  Retry " + ctx.getRetryCount());
                            ctx.getLastThrowable().printStackTrace();
                            return null;
                        }
                );
            }
            System.err.println("Command is completed successfully");
        } finally {
            shutdownInstances(spotInstanceIds);
        }
    }

    private DescribeSpotInstanceRequestsRequest buildDescribeSpotInstanceRequest(final RequestSpotInstancesResult spotInstancesResult) {
        final List<String> spotInstanceRequestIds = spotInstancesResult.getSpotInstanceRequests().stream()
                .map(SpotInstanceRequest::getSpotInstanceRequestId)
                .collect(Collectors.toList());
        return new DescribeSpotInstanceRequestsRequest().withSpotInstanceRequestIds(spotInstanceRequestIds);
    }

    private void shutdownInstances(final List<String> spotInstanceIds) {
        final TerminateInstancesRequest terminateInstancesRequest = new TerminateInstancesRequest();
        terminateInstancesRequest.setInstanceIds(spotInstanceIds);
        amazonEC2.terminateInstances(terminateInstancesRequest);
        System.err.println("Terminated instances successfully: " + spotInstanceIds);
    }

    private static Arguments parseArgs(final String[] args) {
        final Arguments arguments = new Arguments();
        final CmdLineParser parser = new CmdLineParser(arguments);
        try {
            parser.parseArgument(args);
        } catch (final CmdLineException e) {
            System.err.println(e);
            System.exit(1);
        }
        return arguments;
    }

    private static class Arguments {
        @Option(name = "--imageId")
        private String imageId = "ami-0ce107ae7af2e92b5"; // AMI

        @Option(name = "--instanceType")
        private String instanceType = InstanceType.C4Xlarge.name();

        @Option(name = "--iamInstanceProfile")
        private String iamInstanceProfile = "arn:aws:iam::899630095716:instance-profile/access-s3-role";

        @Option(name = "--securityGroup")
        private String securityGroup = "sg-c1a2dca6";

        @Option(name = "--scriptPathInS3")
        private String scriptPathInS3 = "https://s3-ap-northeast-1.amazonaws.com/hiro116s.s3bucket.jp/SoccerTournament/script/bash.sh";

        @Option(name = "--keyName")
        private String keyName = "h-sag-tokyo";

        private InstanceType getInstanceType() {
            return InstanceType.valueOf(instanceType);
        }

        private String getCommandLine() {
            final String[] ws = scriptPathInS3.split("/");
            return ws[ws.length - 1];
        }
    }
}
