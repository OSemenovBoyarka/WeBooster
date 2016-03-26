package com.runjva.sourceforge.jsocks.protocol;

import java.net.InetAddress;

public class PortForwardRule {

	public final int srcPort;

	public final int destPort;

	public final InetAddress destIP;

        public PortForwardRule(int srcPort, int destPort, InetAddress destIP) {
                this.srcPort = srcPort;
                this.destPort = destPort;
                this.destIP = destIP;
        }

        @Override
        public boolean equals(Object o) {
                if (this == o) {
                        return true;
                }
                if (!(o instanceof PortForwardRule)) {
                        return false;
                }
                PortForwardRule that = (PortForwardRule) o;
                return srcPort == that.srcPort;

        }

        @Override
        public int hashCode() {
                return srcPort;
        }
}
