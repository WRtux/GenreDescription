package fxiami.entry;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import com.alibaba.fastjson.JSONObject;

public class SongEntry extends MappedEntry {
	
	public static final String entryName = "song";
	
	protected static final Map<Long, SongEntry> idEntryMap = new HashMap<>();
	protected static final Map<String, SongEntry> sidEntryMap = new HashMap<>();
	
	public static SongEntry getEntry(Long id) {
		return idEntryMap.get(id);
	}
	
	public static SongEntry getEntry(String sid) {
		return sidEntryMap.get(sid);
	}
	
	public static SongEntry getEntry(Long id, String sid) {
		SongEntry en = idEntryMap.get(id);
		if (en != null) {
			if (sid == null || sid.equals(en.sid))
				return en;
			System.err.printf("SID mismatch for ID: %d, SID: %s, expected %s.%n", id, sid, en.sid);
		}
		en = sidEntryMap.get(sid);
		if (en != null) {
			if (id == null || id.equals(en.id))
				return en;
			System.err.printf("ID mismatch for ID: %d, SID: %s, expected %d.%n", id, sid, en.id);
		}
		return null;
	}
	
	public static SongEntry getExactEntry(Long id, String sid) {
		SongEntry en = idEntryMap.get(id);
		if (en != null && !en.dummy) {
			if (Objects.equals(en.sid, sid))
				return en;
			System.err.printf("SID mismatch for ID: %d, SID: %s, expected %s.%n", id, sid, en.sid);
		}
		en = sidEntryMap.get(sid);
		if (en != null && !en.dummy) {
			if (Objects.equals(en.id, id))
				return en;
			System.err.printf("ID mismatch for ID: %d, SID: %s, expected %d.%n", id, sid, en.id);
		}
		return null;
	}
	
	public static Collection<SongEntry> getAll() {
		Set<SongEntry> set = new HashSet<>(idEntryMap.values());
		set.addAll(sidEntryMap.values());
		return set;
	}
	
	public static void clearAll() {
		idEntryMap.clear();
		sidEntryMap.clear();
	}
	
	public static SongEntry matchEntry(Long id, String sid) {
		SongEntry en = idEntryMap.get(id);
		if (en != null) {
			if (sid == null || sid.equals(en.sid))
				return en;
			System.err.printf("SID mismatch for ID: %d, SID: %s, expected %s.%n", id, sid, en.sid);
			if (!en.dummy)
				return null;
		}
		en = sidEntryMap.get(sid);
		if (en != null) {
			if (id == null || id.equals(en.id))
				return en;
			System.err.printf("ID mismatch for ID: %d, SID: %s, expected %d.%n", id, sid, en.id);
			if (!en.dummy)
				return null;
		}
		return new SongEntry(id, sid, true);
	}
	
	public static SongEntry matchDummyEntry(Long id, String sid) {
		SongEntry en = idEntryMap.get(id);
		if (en != null) {
			if (!en.dummy)
				return null;
			if (sid == null || sid.equals(en.sid))
				return en;
			System.err.printf("SID mismatch for ID: %d, SID: %s, expected %s.%n", id, sid, en.sid);
		}
		en = sidEntryMap.get(sid);
		if (en != null) {
			if (!en.dummy)
				return null;
			if (id == null || id.equals(en.id))
				return en;
			System.err.printf("ID mismatch for ID: %d, SID: %s, expected %d.%n", id, sid, en.id);
		}
		return new SongEntry(id, sid, true);
	}
	
	public String subName;
	public String translation;
	
	public ReferenceEntry artist;
	public StaffEntry[] staffs;
	
	public ReferenceEntry album;
	
	public Long disc;
	public Long track;
	
	public Long length;
	
	public Long pace;
	
	public Long highlightOffset;
	public Long highlightLength;
	
	public StyleEntry[] styles;
	
	public String[][] tags;
	
	public InfoEntry[] infos;
	
	public LyricEntry[] lyrics;
	
	public Long playCount;
	public Long likeCount;
	public Long commentCount;
	
	protected SongEntry(Long id, String sid, boolean dummy) {
		super(id, sid, dummy);
		if (this.id != null)
			idEntryMap.put(id, this);
		if (this.sid != null)
			sidEntryMap.put(sid, this);
	}
	public SongEntry(Long id, String sid) {
		this(id, sid, false);
	}
	
	public static SongEntry parseJSON(JSONObject cont) {
		SongEntry en = new SongEntry(cont.getLong("id"), cont.getString("sid"));
		en.update = Helper.parseValidInteger(cont, "update");
		en.name = Helper.parseValidString(cont, "name");
		en.subName = Helper.parseValidString(cont, "subName");
		en.translation = Helper.parseValidString(cont, "translation");
		en.artist = EntryPort.parseJSON(ReferenceEntry.class, cont.getJSONObject("artist"));
		if (cont.containsKey("staffs"))
			en.staffs = EntryPort.parseJSONArray(StaffEntry.class, cont.getJSONArray("staffs"));
		en.album = EntryPort.parseJSON(ReferenceEntry.class, cont.getJSONObject("album"));
		en.disc = Helper.parseValidInteger(cont, "disc");
		en.track = Helper.parseValidInteger(cont, "track");
		en.length = Helper.parseValidInteger(cont, "length");
		en.pace = Helper.parseValidInteger(cont, "pace");
		en.highlightOffset = Helper.parseValidInteger(cont, "highlightOffset");
		en.highlightLength = Helper.parseValidInteger(cont, "highlightLength");
		if (cont.containsKey("styles"))
			en.styles = EntryPort.parseJSONArray(StyleEntry.class, cont.getJSONArray("styles"));
		if (cont.containsKey("tags")) {
			String[][] tags = cont.getObject("tags", String[][].class);
			en.tags = (tags != null) ? tags : EntryPort.forNullEntry(String[][].class);
		}
		if (cont.containsKey("infos"))
			en.infos = EntryPort.parseJSONArray(InfoEntry.class, cont.getJSONArray("infos"));
		if (cont.containsKey("lyrics"))
			en.lyrics = EntryPort.parseJSONArray(LyricEntry.class, cont.getJSONArray("lyrics"));
		en.playCount = Helper.parseValidInteger(cont, "playCount");
		en.likeCount = Helper.parseValidInteger(cont, "likeCount");
		en.commentCount = Helper.parseValidInteger(cont, "commentCount");
		return en;
	}
	
	@Override
	public JSONObject toJSON() {
		JSONObject o = super.toJSON();
		Helper.putValidString(o, "subName", this.subName);
		Helper.putValidString(o, "translation", this.translation);
		o.put("artist", EntryPort.toJSON(this.artist));
		if (this.staffs != null)
			o.put("staffs", EntryPort.toJSONArray(this.staffs));
		o.put("album", EntryPort.toJSON(this.album));
		Helper.putValidInteger(o, "disc", this.disc);
		Helper.putValidInteger(o, "track", this.track);
		Helper.putValidInteger(o, "length", this.length);
		Helper.putValidInteger(o, "pace", this.pace);
		Helper.putValidInteger(o, "highlightOffset", this.highlightOffset);
		Helper.putValidInteger(o, "highlightLength", this.highlightLength);
		if (this.styles != null)
			o.put("styles", EntryPort.toJSONArray(this.styles));
		Helper.putValidArray(o, "tags", this.tags);
		if (this.infos != null)
			o.put("infos", EntryPort.toJSONArray(this.infos));
		if (this.lyrics != null)
			o.put("lyrics", EntryPort.toJSONArray(this.lyrics));
		Helper.putValidInteger(o, "playCount", this.playCount);
		Helper.putValidInteger(o, "likeCount", this.likeCount);
		Helper.putValidInteger(o, "commentCount", this.commentCount);
		return o;
	}
	
}
