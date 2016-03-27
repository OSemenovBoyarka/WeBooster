package com.uawebchallenge.webooster.util;

import java.util.Collection;
import java.util.Iterator;

/**
 * @author Alexander Semenov
 */
public class StringUtils {

    public static String join(Collection<?> col, String separator) {
        StringBuilder sb = new StringBuilder();
        Iterator<?> iter = col.iterator();
        if (iter.hasNext())
            sb.append(iter.next().toString());
        while (iter.hasNext()) {
            sb.append(separator);
            sb.append(iter.next().toString());
        }
        return sb.toString();
    }

}
