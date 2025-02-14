package fxiami.entry;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

public final class Helper {
	
	public static boolean isNullFloatValue(double num) {
		return Double.doubleToRawLongBits(num) == Double.doubleToRawLongBits(EntryPort.NULL_FLOAT);
	}
	
	public static boolean isNullArray(Object[] arr) {
		return arr != null && arr == EntryPort.forNullEntry(arr.getClass());
	}
	
	public static boolean isEmptyArray(Object[] arr) {
		return arr != null && arr != EntryPort.forNullEntry(arr.getClass()) && arr.length == 0;
	}
	
	public static Long parseValidInteger(JSONObject o, String k) {
		if (!o.containsKey(k))
			return null;
		Long num = null;
		try {
			num = o.getLong(k);
		} catch (RuntimeException ex) {
			System.out.println("Not an integer: " + String.valueOf(o.get(k)));
		}
		return num != null ? num : EntryPort.NULL_INTEGER;
	}
	
	public static Double parseValidFloat(JSONObject o, String k) {
		if (!o.containsKey(k))
			return null;
		Double num = null;
		try {
			num = o.getDouble(k);
		} catch (RuntimeException ex) {
			System.out.println("Not a float: " + String.valueOf(o.get(k)));
		}
		return num != null ? num : EntryPort.NULL_FLOAT;
	}
	
	public static Boolean parseValidBoolean(JSONObject o, String k) {
		if (!o.containsKey(k))
			return null;
		Boolean b = null;
		try {
			b = o.getBoolean(k);
		} catch (RuntimeException ex) {
			System.out.println("Not a boolean: " + String.valueOf(o.get(k)));
		}
		return b != null ? b : EntryPort.NULL_BOOLEAN;
	}
	
	public static String parseValidString(JSONObject o, String k) {
		if (!o.containsKey(k))
			return null;
		String str = null;
		try {
			str = o.getString(k);
		} catch (RuntimeException ex) {
			System.out.println("Not a string: " + String.valueOf(o.get(k)));
		}
		return str != null ? str : EntryPort.NULL_STRING;
	}
	
	public static Object[] parseValidArray(JSONObject o, String k) {
		if (!o.containsKey(k))
			return null;
		Object[] arr = null;
		try {
			JSONArray cont = o.getJSONArray(k);
			if (cont != null)
				arr = cont.toArray();
		} catch (RuntimeException ex) {
			System.out.println("Not a valid array: " + String.valueOf(o.get(k)));
		}
		return arr != null ? arr : EntryPort.NULL_OBJECT_ARRAY;
	}
	
	@SuppressWarnings("unchecked")
	public static <T> T[] parseValidArray(JSONObject o, String k, T[] dest) {
		if (!o.containsKey(k))
			return null;
		T[] arr = null;
		try {
			JSONArray cont = o.getJSONArray(k);
			if (cont != null)
				arr = cont.toArray(dest);
		} catch (RuntimeException ex) {
			System.out.println("Not a valid array: " + String.valueOf(o.get(k)));
		}
		return arr != null ? arr : (T[])EntryPort.forNullEntry(dest.getClass());
	}
	
	public static ReferenceEntry parseValidEntry(JSONObject o, String idk, String sidk) {
		ReferenceEntry ren = null;
		try {
			Long id = o.getLong(idk);
			String sid = o.getString(sidk);
			if (id != null || sid != null)
				ren = new ReferenceEntry(id, sid);
		} catch (RuntimeException ex) {
			System.out.printf("Not a valid entry: %s, %s%n",
				String.valueOf(o.get(idk)), String.valueOf(o.get(sidk)));
		}
		return ren;
	}
	
	public static ReferenceEntry parseValidEntry(JSONObject o, String idk, String sidk, String nk) {
		ReferenceEntry ren = null;
		try {
			Long id = o.getLong(idk);
			String sid = o.getString(sidk);
			String n = parseValidString(o, nk);
			if (id != null || sid != null || (n != null && n != EntryPort.NULL_STRING))
				ren = new ReferenceEntry(id, sid, n);
		} catch (RuntimeException ex) {
			System.out.printf("Not a valid entry: %s, %s, %s%n",
				String.valueOf(o.get(idk)), String.valueOf(o.get(sidk)), String.valueOf(o.get(nk)));
		}
		return ren;
	}
	
	public static boolean putValidInteger(JSONObject dest, String k, Number num) {
		if (num == null)
			return false;
		dest.put(k, num != EntryPort.NULL_INTEGER ? num.longValue() : null);
		return true;
	}
	
	public static boolean putValidFloat(JSONObject dest, String k, Number num) {
		if (num == null)
			return false;
		dest.put(k, num != EntryPort.NULL_FLOAT ? num.doubleValue() : null);
		return true;
	}
	
	public static boolean putValidBoolean(JSONObject dest, String k, Boolean b) {
		if (b == null)
			return false;
		dest.put(k, b != EntryPort.NULL_BOOLEAN ? b : null);
		return true;
	}
	
	public static boolean putValidString(JSONObject dest, String k, String str) {
		if (str == null)
			return false;
		dest.put(k, str != EntryPort.NULL_STRING ? str : null);
		return true;
	}
	
	public static boolean putValidArray(JSONObject dest, String k, Object[] arr) {
		if (arr == null)
			return false;
		dest.put(k, arr != EntryPort.forNullEntry(arr.getClass()) ? arr : null);
		return true;
	}
	
	public static boolean putValidEntry(JSONObject dest, String idk, String sidk, MappedEntry en) {
		if (en == null)
			return false;
		dest.put(idk, en.id);
		dest.put(sidk, en.sid);
		return true;
	}
	public static boolean putValidEntry(JSONObject dest, MappedEntry en) {
		return putValidEntry(dest, "id", "sid", en);
	}
	
	public static JSONObject getValidEntry(String idk, String sidk, MappedEntry en) {
		if (en == null)
			return null;
		JSONObject o = new JSONObject(true);
		o.put(idk, en.id);
		o.put(sidk, en.sid);
		return o;
	}
	public static JSONObject getValidEntry(MappedEntry en) {
		return getValidEntry("id", "sid", en);
	}
	
	@Deprecated
	private Helper() {
		throw new IllegalStateException();
	}
	
}
