package com.openexchange.push.mail.notify;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import com.openexchange.configuration.ConfigurationException;
import com.openexchange.push.PushException;


/**
 * {@link MailNotifyPushUdpSocketListener} - A socket listener which receives
 * UDP packets and generates events
 *
 */
public class MailNotifyPushUdpSocketListener implements Runnable {

    private static final org.apache.commons.logging.Log LOG = org.apache.commons.logging.LogFactory.getLog(MailNotifyPushUdpSocketListener.class);

    private static final int MAX_UDP_PACKET_SIZE = 4+64+1;
    
    private static DatagramSocket datagramSocket;
    
    private final String imapLoginDelimiter;
    
    public MailNotifyPushUdpSocketListener(final String udpListenHost, final int udpListenPort, final String imapLoginDelimiter) throws UnknownHostException, SocketException, ConfigurationException {
        InetAddress senderAddress = InetAddress.getByName(udpListenHost);

        this.imapLoginDelimiter = imapLoginDelimiter;
        if (senderAddress != null) {
            datagramSocket = new DatagramSocket(udpListenPort, senderAddress);
        } else {
            throw new ConfigurationException(ConfigurationException.Code.INVALID_CONFIGURATION, "Can't get internet addres to given hostname " + udpListenHost);
        }
    }
    
    private void start() {
        while (true) {
            final DatagramPacket datagramPacket = new DatagramPacket(new byte[MAX_UDP_PACKET_SIZE], MAX_UDP_PACKET_SIZE);
            try {
                datagramSocket.receive(datagramPacket);

                if (datagramPacket.getLength() > 0) {
                    // Packet received
                    final String mailboxName = getMailboxName(datagramPacket);
                    MailNotifyPushListenerRegistry.getInstance().fireEvent(mailboxName);
                } else {
                    LOG.warn("recieved empty udp package: " + datagramSocket);
                }
            } catch (final IOException e) {
                LOG.error("Receiving of UDP packet failed: " + e.getMessage(), e);
            } catch (final PushException e) {
                LOG.error("Failed to create push event: " + e.getMessage(), e);
            }
        }
    }

    private String getMailboxName(DatagramPacket datagramPacket) {
        /* TODO: this currently works with cyrus notify must be configurable somehow later
         * 
         * Format:
         *   notifyd/notifyd.c:
         *   method NUL class NUL priority NUL user NUL mailbox NUL
         *   nopt NUL N(option NUL) message NUL
         *   
         * Example:
         * 
         *  log\0MAIL\0\0postmaster\0INBOX\00\0From: root@oxsles11.example.com (root)
         *  Subject: asdf To: postmaster@example.com 
         */

        String packetDataString = new String(datagramPacket.getData());
        // user name at position 3, see above
        packetDataString = packetDataString.split("\0")[3];
        if (null != imapLoginDelimiter) {
        	final int idx;
        	idx = packetDataString.indexOf(imapLoginDelimiter);
        	if (idx != -1) {
        		packetDataString = packetDataString.substring(0, idx);
        	}
        }
        LOG.debug("Username=" + packetDataString);
        if (null != packetDataString && packetDataString.length() > 0) {
            return packetDataString;
        } else {
            return null;
        }
    }

    public void run() {
        start();
    }
}
