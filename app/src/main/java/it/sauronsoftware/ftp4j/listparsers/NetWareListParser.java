package it.sauronsoftware.ftp4j.listparsers;

import it.sauronsoftware.ftp4j.FTPFile;
import it.sauronsoftware.ftp4j.FTPListParseException;
import it.sauronsoftware.ftp4j.FTPListParser;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class NetWareListParser implements FTPListParser {
    private static final Pattern PATTERN = Pattern.compile("^(d|-)\\s+\\[.{8}\\]\\s+\\S+\\s+(\\d+)\\s+(?:(\\w{3})\\s+(\\d{1,2}))\\s+(?:(\\d{4})|(?:(\\d{1,2}):(\\d{1,2})))\\s+([^\\\\/*?\"<>|]+)$");
    private static final DateFormat DATE_FORMAT;

    public NetWareListParser() {
    }

    public FTPFile[] parse(String[] lines) throws FTPListParseException {
        int size = lines.length;
        Calendar now = Calendar.getInstance();
        int currentYear = now.get(1);
        FTPFile[] ret = new FTPFile[size];

        for(int i = 0; i < size; ++i) {
            Matcher m = PATTERN.matcher(lines[i]);
            if (!m.matches()) {
                throw new FTPListParseException();
            }

            String typeString = m.group(1);
            String sizeString = m.group(2);
            String monthString = m.group(3);
            String dayString = m.group(4);
            String yearString = m.group(5);
            String hourString = m.group(6);
            String minuteString = m.group(7);
            String nameString = m.group(8);
            ret[i] = new FTPFile();
            if (typeString.equals("-")) {
                ret[i].setType(0);
            } else {
                if (!typeString.equals("d")) {
                    throw new FTPListParseException();
                }

                ret[i].setType(1);
            }

            long fileSize;
            try {
                fileSize = Long.parseLong(sizeString);
            } catch (Throwable var25) {
                throw new FTPListParseException();
            }

            ret[i].setSize(fileSize);
            if (dayString.length() == 1) {
                dayString = "0" + dayString;
            }

            StringBuffer mdString = new StringBuffer();
            mdString.append(monthString);
            mdString.append(' ');
            mdString.append(dayString);
            mdString.append(' ');
            boolean checkYear = false;
            if (yearString == null) {
                mdString.append(currentYear);
                checkYear = true;
            } else {
                mdString.append(yearString);
                checkYear = false;
            }

            mdString.append(' ');
            if (hourString != null && minuteString != null) {
                if (hourString.length() == 1) {
                    hourString = "0" + hourString;
                }

                if (minuteString.length() == 1) {
                    minuteString = "0" + minuteString;
                }

                mdString.append(hourString);
                mdString.append(':');
                mdString.append(minuteString);
            } else {
                mdString.append("00:00");
            }

            Date md;
            try {
                synchronized(DATE_FORMAT) {
                    md = DATE_FORMAT.parse(mdString.toString());
                }
            } catch (ParseException var24) {
                throw new FTPListParseException();
            }

            if (checkYear) {
                Calendar mc = Calendar.getInstance();
                mc.setTime(md);
                if (mc.after(now) && mc.getTimeInMillis() - now.getTimeInMillis() > 86400000L) {
                    mc.set(1, currentYear - 1);
                    md = mc.getTime();
                }
            }

            ret[i].setModifiedDate(md);
            ret[i].setName(nameString);
        }

        return ret;
    }

    static {
        DATE_FORMAT = new SimpleDateFormat("MMM dd yyyy HH:mm", Locale.US);
    }
}
