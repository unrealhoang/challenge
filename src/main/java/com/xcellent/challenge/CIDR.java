package com.xcellent.challenge;

import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * Example Implementation of a CIDR.
 * 
 * @see "http://en.wikipedia.org/wiki/Classless_Inter-Domain_Routing"
 * 
 * @author stefan.majer@x-cellent.com
 * 
 */
public class CIDR {
	private InetAddress address;
	private Integer mask;
	
	/**
	 * Constructor
	 * 
	 * @param address the input ipAddress
	 * @param mask the mask
	 */
	private CIDR(InetAddress address, Integer mask) {
		this.address = address;
		this.mask = mask;
	}
	
	/**
	 * Create a new Instance of a CIDR from a given String in cidr form.
	 * 
	 * This is either in the form 1.2.3.4/24 or 1.2.3.4/255.255.255.0
	 * 
	 * @param cidrNotation
	 *            the input ipAddress/network in cidr notation.
	 * @return the CIDR instance, throws a appropriate Exception on malformed or illegal input.
	 */
	public static CIDR of(String cidrNotation) {
		String[] cidr = cidrNotation.split("/");
		CIDR result = null;
		try {
			InetAddress tAddr = InetAddress.getByName(cidr[0]);
			Integer tMask = Integer.parseInt(cidr[1]);
			result = CIDR.of(tAddr, tMask);
		} catch (UnknownHostException e) {
			throw new RuntimeException("Illegal input");
		} catch (NumberFormatException e) {
			throw new RuntimeException("Illegal input");
		}
		return result;
	}

	/**
	 * Create a new Instance from a InetAddress and a Mask.
	 * 
	 * @param inetAddress
	 *            the InetAddress
	 * @param mask
	 *            the mask
	 * @return a CIDR instance.
	 */
	public static CIDR of(InetAddress inetAddress, Integer mask) {
		return new CIDR(inetAddress, mask);
	}

	/**
	 * @return the Address part of this CIDR.
	 */
	public String getAddress() {
		return address.getHostAddress();
	}

	/**
	 * @return the Mask (0 - 32) of this CIDR.
	 */
	public Integer getMask() {
		return mask;
	}

	/**
	 * Get byte from a full byte presentation of a network address
	 * @param i index of the byte to get
	 * @param source the integer source
	 * @return the byte at index i of source
	 */
	private static byte getByte(int i, int source) {
		if (i > 3)
			throw new UnsupportedOperationException();
		
		int shifted = (3 - i) * 8;
		return (byte) ((source >> shifted) & 0xFF);
	}
	
	/**
	 * @param addr InetAddress to sum up
	 * @return Convert byte array address to single int for calculation
	 */
	private static int getFullBytePresentation(InetAddress addr) {
		int result = 0;
		byte[] addrByte = addr.getAddress();
		for (int i = 0; i < 4; i++) 
			result += addrByte[i] << (8 * (3 - i));
		return result;
	}
	
	/**
	 * @param source int source to separate to byte array
	 * @return byte array presentation of the byte address source
	 */
	private static byte[] getByteArrayPresentation(int source) {
		byte[] result = new byte[4];
		for (int i = 0; i < 4; i++) 
			result[i] = getByte(i, source);
		return result;
	}
	
	/**
	 * @return the mask address of this CIDR
	 */
	private InetAddress getMaskAddress() {
		// Get the byte presentation of the mask address
		int i = 0xFFFFFFFF << (32 - mask);
		byte[] b = new byte[] {getByte(0, i), getByte(1, i), getByte(2, i), getByte(3, i)};
		InetAddress result = null;
		
		try {
			result = InetAddress.getByAddress(b);
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return result;
	}
	
	/**
	 * @return the Network address of this CIDR
	 */
	public CIDR getNetwork() {
		// Get the byte presentation of the Network address by
		// and-ing the IP address with the mask
		byte[] maskByte = getMaskAddress().getAddress();
		byte[] addrByte = address.getAddress();
		
		byte[] b = new byte[4];
		for (int i = 0; i < 4; i++) 
			b[i] = (byte) (maskByte[i] & addrByte[i]);
		
		InetAddress networkAddr = null;
		try {
			networkAddr = InetAddress.getByAddress(b);
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return CIDR.of(networkAddr, mask);
	}

	/**
	 * @return the BroadCast Address of this CIDR
	 */
	public CIDR getBroadCast() {
		InetAddress maskAddr = getMaskAddress();
		int invertedMask = ~getFullBytePresentation(maskAddr);
		int addr = getFullBytePresentation(address);
		int broadcastAddr = invertedMask | addr;
		
		InetAddress result = null;
		
		try {
			result = InetAddress.getByAddress(getByteArrayPresentation(broadcastAddr));
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return CIDR.of(result, mask);
	}

	/**
	 * @return the amount of Addresses available in this network.
	 */
	public Long getAddresses() {
		return (long) 1 << (32 - mask);
	}

	/**
	 * @return the next IP in this Network throws a Exception if there is no ip left.
	 */
	public CIDR getNext() {
		if (address.equals(getBroadCast())) {
			throw new RuntimeException("There is no more IP left in this network");
		}
		
		int curAddr = getFullBytePresentation(address);
		byte[] nextAddrByte = getByteArrayPresentation(curAddr + 1); 
		
		InetAddress nextAddr = null;
		try {
			nextAddr = InetAddress.getByAddress(nextAddrByte);
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			throw new RuntimeException("There is no more IP left in this network");
		}
		return CIDR.of(nextAddr, mask);
	}

	/**
	 * @return the previoud IP in this Network throws a Exception if there is no ip left.
	 */
	public CIDR getPrevious() {
		if (address.equals(getNetwork().address)) {
			throw new RuntimeException("There is no more IP left in this network");
		}
		
		int curAddr = getFullBytePresentation(address);
		byte[] nextAddrByte = getByteArrayPresentation(curAddr - 1); 
		
		InetAddress nextAddr = null;
		try {
			nextAddr = InetAddress.getByAddress(nextAddrByte);
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			throw new RuntimeException("There is no more IP left in this network");
		}
		return CIDR.of(nextAddr, mask);
	}

	/**
	 * Check if a other cidr is inside this cidr.
	 * 
	 * @param cidr
	 *            the cidr which is checked against this cidr.
	 * @return true if the given cidr is inside this cidr, otherwise false.
	 */
	public boolean contains(CIDR cidr) {
		if (cidr.getMask().equals(mask))
			return false;
		
		int networkAddrByte = getFullBytePresentation(getNetwork().address);
		int broadcastAddrByte = getFullBytePresentation(getBroadCast().address);
		int addrByte = getFullBytePresentation(cidr.address);
		
		if (addrByte >= networkAddrByte && addrByte <= broadcastAddrByte)
			return true;
		return false;
	}

	/**
	 * This returns a String representation of this CIDR with a appropriate formatting. So that a
	 * given CIDR for example 1.2.3.4/24 will return 1.2.3.0/24.
	 * 
	 * /32 CIDRs can ommit the trailing mask.
	 * 
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		if (mask == 32)
			return address.getHostAddress();
		return address.getHostAddress() + "/" + mask;
	}
	
	/**
	 * Override equals for comparing CIDRs
	 * @return true if and only if the object comparing is CIRD 
	 * and have same address and mask
	 */
	@Override
	public boolean equals(Object o) {
		if (!(o instanceof CIDR))
			return false;
		CIDR t = (CIDR) o;
		if (!t.address.equals(address) || !t.mask.equals(mask))
			return false;
		return true;
	}

}
