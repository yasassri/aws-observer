package org.ycr.aws;

import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.AmazonEC2ClientBuilder;
import com.amazonaws.services.ec2.model.DescribeInstancesRequest;
import com.amazonaws.services.ec2.model.DescribeInstancesResult;
import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.Reservation;
import com.amazonaws.services.ec2.model.Tag;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class Main {

    public static void main(String[] args) {

        StringBuilder tempalte = new StringBuilder();
        List<String> excludeIdList = new ArrayList<>();
        excludeIdList.add("i-0e637e411a26a3cd4");
        excludeIdList.add("i-0f978eabef81c5b20");
        excludeIdList.add("i-0e937ecb63213d21b");
        tempalte.append("<!DOCTYPE html>\n" +
                        "<html>\n" +
                        "<head>\n" +
                        "</head>\n" +
                        "<body>\n" +
                        "\n" +
                        "<table id=\"customers\" border=\"1\" style=\"font-family: \"Trebuchet MS\", Arial, Helvetica, sans-serif; width: 100%;\">\n" +
                        "  <tr>\n" +
                        "    <th>Instance Name</th>\n" +
                        "    <th>Instance ID</th>\n" +
                        "    <th>State</th>\n" +
                        "    <th>Instance Type</th>\n" +
                        "    <th>Started Time Stamp</th>\n" +
                        "    <th>Associated Stack</th>\n" +
                        "    <th>Uptime(Hours)</th>\n" +
                        "  </tr>\n");
        final AmazonEC2 ec2 = AmazonEC2ClientBuilder.defaultClient();
        boolean done = false;
        boolean runningInstancesAvailable = false;

        DescribeInstancesRequest request = new DescribeInstancesRequest();
        while (!done) {
            DescribeInstancesResult response = ec2.describeInstances(request);

            for (Reservation reservation : response.getReservations()) {
                boolean exclude = false;
                for (Instance instance : reservation.getInstances()) {
                    for (String id : excludeIdList) {
                        if (id.equals(instance.getInstanceId())) {
                            exclude = true;
                            break;
                        }
                    }
                    if (!exclude) {
                        if (instance.getState().getName().equals("running") && !instance.getInstanceType().equals
                                ("t2.micro")) {
                            runningInstancesAvailable = true;
                            // Get the insatance name
                            String instanceName = "Unknown";
                            String stackName = "No Stack!";
                            if (instance.getTags() != null) {
                                for (Tag tag : instance.getTags()) {
                                    if (tag.getKey().equals("Name")) {
                                        instanceName = tag.getValue();
                                        System.out.println("Instance Name : " + instanceName);
                                    }
                                    if (tag.getKey().equals("aws:cloudformation:stack-name")){
                                        stackName = tag.getValue();
                                    }
                                }
                            }
                            long uptime = getDateDiff(instance.getLaunchTime(), new Date(), TimeUnit.HOURS);
                            if (uptime > 2) {
                                tempalte.append("  <tr style=\"color: #f71313; font-weight:bold\">\n" +
                                                "    <td>" + instanceName + "</td>\n" +
                                                "    <td>" + instance.getInstanceId() + "</td>\n" +
                                                "    <td>" + instance.getState().getName() + "</td>\n" +
                                                "    <td>" + instance.getInstanceType() + "</td>\n" +
                                                "    <td>" + instance.getLaunchTime().toString() + "</td>\n" +
                                                "    <td>" + stackName + "</td>\n" +
                                                "    <td style=\"text-align:center\">" + uptime + "</td>\n" +
                                                "  </tr>\n");

                            } else {
                                tempalte.append("  <tr>\n" +
                                                "    <td>" + instanceName + "</td>\n" +
                                                "    <td>" + instance.getInstanceId() + "</td>\n" +
                                                "    <td>" + instance.getState().getName() + "</td>\n" +
                                                "    <td>" + instance.getInstanceType() + "</td>\n" +
                                                "    <td>" + instance.getLaunchTime().toString() + "</td>\n" +
                                                "    <td>" + stackName + "</td>\n" +
                                                "    <td style=\"text-align:center\">" + uptime + "</td>\n" +
                                                "  </tr>\n");
                            }

                            System.out.printf(
                                    "Found instance with id %s, " +
                                    "AMI %s, " +
                                    "type %s, " +
                                    "state %s " +
                                    "and monitoring state %s",
                                    instance.getInstanceId(),
                                    instance.getImageId(),
                                    instance.getInstanceType(),
                                    instance.getState().getName(),
                                    instance.getMonitoring().getState());
                        }
                        instance.getKeyName();
                    }
                }
            }
            request.setNextToken(response.getNextToken());
            if (response.getNextToken() == null) {
                done = true;
            }
        }
        tempalte.append("</table>\n" +
                        "</body>\n" +
                        "</html>");
        System.out.println(tempalte);
        if (runningInstancesAvailable){
            File file = new File("./mail.html");
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
                writer.write(tempalte.toString());
                writer.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Get a diff between two dates
     * @param date1 the oldest date
     * @param date2 the newest date
     * @param timeUnit the unit in which you want the diff
     * @return the diff value, in the provided unit
     */
    public static long getDateDiff(Date date1, Date date2, TimeUnit timeUnit) {
        long diffInMillies = date2.getTime() - date1.getTime();
        return timeUnit.convert(diffInMillies,TimeUnit.MILLISECONDS);
    }
}
