package org.jahia.configuration.cluster.tool.operations;

import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.model.DescribeInstancesResult;
import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.Reservation;
import com.amazonaws.services.ec2.model.Tag;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.SftpException;
import org.jahia.configuration.cluster.tool.ClusterConfigBean;
import org.jahia.configuration.configurators.PropertiesManager;
import org.jahia.configuration.logging.AbstractLogger;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Small helper class to quickly retrieve a list of EC2 instances, their public DNS names and internals IPs to avoid
 * copy-pasting the list by hand.
 */
public class AWSGetClusterInstances extends AbstractClusterOperation {

    public AWSGetClusterInstances(AbstractLogger logger, ClusterConfigBean clusterConfigBean) {
        super(logger, clusterConfigBean);
    }

    @Override
    public void execute() throws JSchException, SftpException, IOException {
        BasicAWSCredentials basicAWSCredentials = new BasicAWSCredentials(clusterConfigBean.getAwsAccessKey(), clusterConfigBean.getAwsSecretKey());
        AmazonEC2Client amazonEC2Client = new AmazonEC2Client(basicAWSCredentials);
        DescribeInstancesResult describeInstancesResult = amazonEC2Client.describeInstances();
        List<String> externalHostNames = new ArrayList<String>();
        List<String> internalIPs = new ArrayList<String>();
        StringBuffer publicDnsNames = new StringBuffer();
        StringBuffer privateIpAddresses = new StringBuffer();
        StringBuffer nameTags = new StringBuffer();
        if (describeInstancesResult != null) {
            List<Reservation> reservationsList = describeInstancesResult.getReservations();
            for (Reservation reservation : reservationsList) {
                List<Instance> instanceList = reservation.getInstances();
                for (Instance instance: instanceList) {
                    if ("running".equals(instance.getState().getName()) && (externalHostNames.size() < clusterConfigBean.getNumberOfNodes())) {
                        externalHostNames.add(instance.getPublicDnsName());
                        internalIPs.add(instance.getPrivateIpAddress());
                        publicDnsNames.append(instance.getPublicDnsName());
                        publicDnsNames.append(",");
                        privateIpAddresses.append(instance.getPrivateIpAddress());
                        privateIpAddresses.append(",");
                        List<Tag> tags = instance.getTags();
                        for (Tag tag : tags) {
                            if ("Name".equals(tag.getKey())) {
                                nameTags.append(tag.getValue());
                            }
                        }
                        nameTags.append(",");
                    }
                }
            }
        }
        logger.info("Public DNS names = " + publicDnsNames.toString().substring(0, publicDnsNames.toString().length()-1));
        logger.info("Private IP addresses = " + privateIpAddresses.toString().substring(0, privateIpAddresses.toString().length() - 1));
        logger.info("Names = " + nameTags.toString().substring(0, nameTags.toString().length() - 1));

        clusterConfigBean.setExternalHostNames(externalHostNames);
        clusterConfigBean.setInternalIPs(internalIPs);
        clusterConfigBean.store();
    }
}
