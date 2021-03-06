/**
 * Oshi (https://github.com/dblock/oshi)
 *
 * Copyright (c) 2010 - 2016 The Oshi Project Team
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Maintainers:
 * dblock[at]dblock[dot]org
 * widdis[at]gmail[dot]com
 * enrico.bianchi[at]gmail[dot]com
 *
 * Contributors:
 * https://github.com/dblock/oshi/graphs/contributors
 */
package oshi.util;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;

import javax.xml.bind.DatatypeConverter;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * EDID parsing utility.
 * 
 * @author widdis[at]gmail[dot]com
 */
public abstract class EdidUtil {

    private static final Log LOG = LogFactory.getLog(EdidUtil.class);

    /**
     * Converts a byte array to a hexadecimal string
     * 
     * @param edid
     *            The EDID byte array
     * @return A string representation of the byte array
     */
    public static String toString(byte[] edid) {
        return DatatypeConverter.printHexBinary(edid);
    }

    /**
     * Gets the Manufacturer ID from (up to) 3 5-bit characters in bytes 8 and 9
     * 
     * @param edid
     *            The EDID byte array
     * @return The manufacturer ID
     */
    public static String getManufacturerID(byte[] edid) {
        // Bytes 8-9 are manufacturer ID in 3 5-bit characters.
        String temp = String
                .format("%8s%8s", Integer.toBinaryString(edid[8] & 0xFF), Integer.toBinaryString(edid[9] & 0xFF))
                .replace(' ', '0');
        LOG.debug("Manufacurer ID: " + temp);
        return String.format("%s%s%s", (char) (64 + Integer.parseInt(temp.substring(1, 6), 2)),
                (char) (64 + Integer.parseInt(temp.substring(7, 11), 2)),
                (char) (64 + Integer.parseInt(temp.substring(12, 16), 2))).replace("@", "");
    }

    /**
     * Gets the Product ID, bytes 10 and 11
     * 
     * @param edid
     *            The EDID byte array
     * @return The product ID
     */
    public static String getProductID(byte[] edid) {
        // Bytes 10-11 are product ID expressed in hex characters
        return Integer.toHexString(
                ByteBuffer.wrap(Arrays.copyOfRange(edid, 10, 12)).order(ByteOrder.LITTLE_ENDIAN).getShort() & 0xffff);
    }

    /**
     * Gets the Serial number, bytes 12-15
     * 
     * @param edid
     *            The EDID byte array
     * @return If all 4 bytes represent alphanumeric characters, a 4-character
     *         string, otherwise a hex string.
     */
    public static String getSerialNo(byte[] edid) {
        // Bytes 12-15 are Serial number (last 4 characters)
        LOG.debug("Serial number: " + Arrays.toString(Arrays.copyOfRange(edid, 12, 16)));
        return String.format("%s%s%s%s", getAlphaNumericOrHex(edid[15]), getAlphaNumericOrHex(edid[14]),
                getAlphaNumericOrHex(edid[13]), getAlphaNumericOrHex(edid[12]));
    }

    private static String getAlphaNumericOrHex(byte b) {
        return Character.isLetterOrDigit((char) b) ? String.format("%s", (char) b) : String.format("%02X", b);
    }

    /**
     * Return the week of year of manufacture
     * 
     * @param edid
     *            The EDID byte array
     * @return The week of year
     */
    public static byte getWeek(byte[] edid) {
        // Byte 16 is manufacture week
        return edid[16];
    }

    /**
     * Return the year of manufacture
     * 
     * @param edid
     *            The EDID byte array
     * @return The year of manufacture
     */
    public static int getYear(byte[] edid) {
        // Byte 17 is manufacture year-1990
        byte temp = edid[17];
        LOG.debug("Year-1990: " + temp);
        return temp + 1990;
    }

    /**
     * Return the EDID version
     * 
     * @param edid
     *            The EDID byte array
     * @return The EDID version
     */
    public static String getVersion(byte[] edid) {
        // Bytes 18-19 are EDID version
        return edid[18] + "." + edid[19];
    }

    /**
     * Test if this EDID is a digital monitor based on byte 20
     * 
     * @param edid
     *            The EDID byte array
     * @return True if the EDID represents a digital monitor, false otherwise
     */
    public static boolean isDigital(byte[] edid) {
        // Byte 20 is Video input params
        return 1 == edid[20] >> 7;
    }

    /**
     * Get monitor width in cm
     * 
     * @param edid
     *            The EDID byte array
     * @return Monitor width in cm
     */
    public static int getHcm(byte[] edid) {
        // Byte 21 is horizontal size in cm
        return edid[21];
    }

    /**
     * Get monitor height in cm
     * 
     * @param edid
     *            The EDID byte array
     * @return Monitor height in cm
     */
    public static int getVcm(byte[] edid) {
        // Byte 22 is vertical size in cm
        return edid[22];
    }

    /**
     * Get the VESA descriptors
     * 
     * @param edid
     *            The EDID byte array
     * @return A 2D array with four 18-byte elements representing VESA
     *         descriptors
     */
    public static byte[][] getDescriptors(byte[] edid) {
        byte[][] desc = new byte[4][18];
        for (int i = 0; i < desc.length; i++) {
            System.arraycopy(edid, 54 + 18 * i, desc[i], 0, 18);
        }
        return desc;
    }

    /**
     * Get the VESA descriptor type
     * 
     * @param desc
     *            An 18-byte VESA descriptor
     * @return An integer representing the first four bytes of the VESA
     *         descriptor
     */
    public static int getDescriptorType(byte[] desc) {
        return ByteBuffer.wrap(Arrays.copyOfRange(desc, 0, 4)).getInt();
    }

    /**
     * Parse a detailed timing descriptor
     * 
     * @param desc
     *            An 18-byte VESA descriptor
     * @return A string describing part of the detailed timing descriptor
     */
    public static String getTimingDescriptor(byte[] desc) {
        int clock = ByteBuffer.wrap(Arrays.copyOfRange(desc, 0, 2)).order(ByteOrder.LITTLE_ENDIAN).getShort() / 100;
        int hActive = (desc[2] & 0xff) + (desc[4] & 0xf0) << 4;
        int vActive = (desc[5] & 0xff) + (desc[7] & 0xf0) << 4;
        return String.format("Clock %dMHz, Active Pixels %dx%d ", clock, hActive, vActive);
    }

    /**
     * Parse descriptor range limits
     * 
     * @param desc
     *            An 18-byte VESA descriptor
     * @return A string describing some of the range limits
     */
    public static String getDescriptorRangeLimits(byte[] desc) {
        return String.format("Field Rate %d-%d Hz vertical, %d-%d Hz horizontal, Max clock: %d MHz", desc[5], desc[6],
                desc[7], desc[8], desc[9] * 10);
    }

    /**
     * Parse descriptor text
     * 
     * @param desc
     *            An 18-byte VESA descriptor
     * @return Plain text starting at the 4th byte
     */
    public static String getDescriptorText(byte[] desc) {
        return new String(Arrays.copyOfRange(desc, 4, 18)).trim();
    }

    /**
     * Parse descriptor hex
     * 
     * @param desc
     *            An 18-byte VESA descriptor
     * @return A string showing the 18 bytes as hexadecimals
     */
    public static String getDescriptorHex(byte[] desc) {
        return DatatypeConverter.printHexBinary(desc);
    }
}
