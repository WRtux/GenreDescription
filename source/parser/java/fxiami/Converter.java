package fxiami;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

import fxiami.entry.AlbumEntry;
import fxiami.entry.ArtistEntry;
import fxiami.entry.CategoryEntry;
import fxiami.entry.EntryPort;
import fxiami.entry.MappedEntry;
import fxiami.entry.Helper;
import fxiami.entry.InfoEntry;
import fxiami.entry.LyricEntry;
import fxiami.entry.ReferenceEntry;
import fxiami.entry.SongEntry;
import fxiami.entry.StaffEntry;
import fxiami.entry.StyleEntry;

public final class Converter {
	
	public static final class ArtistConverter {
		
		static ArtistEntry processEntry(JSONObject cont, boolean ext) {
			Long id = cont.getLong("artistId");
			String sid = cont.getString("artistStringId");
			ArtistEntry en = null;
			if (ext) {
				en = ArtistEntry.getExactEntry(id, sid);
				if (en == null)
					en = new ArtistEntry(id, sid);
			} else {
				en = ArtistEntry.matchDummyEntry(id, sid);
			}
			return en;
		}
		
		static String processGender(JSONObject cont) {
			if (!cont.containsKey("gender"))
				return null;
			try {
				String str = cont.getString("gender");
				switch (str) {
				case "":
					str = EntryPort.NULL_STRING;
					break;
				case "M":
				case "F":
				case "B":
				case "N":
					break;
				default:
					throw new RuntimeException();
				}
				return str;
			} catch (RuntimeException ex) {
				System.out.println("Not a valid gender: " + String.valueOf(cont.get("gender")));
				return EntryPort.NULL_STRING;
			}
		}
		
		static MappedEntry[] processSimilars(String typ, JSONObject cont) {
			String n;
			switch (typ) {
			case "artist":
				n = "similaryArtists";
				break;
			case "album":
				n = "albums";
				break;
			case "song":
				n = "songs";
				break;
			default:
				throw new IllegalArgumentException();
			}
			if (!cont.containsKey(n))
				return null;
			try {
				JSONArray arr = cont.getJSONArray(n);
				MappedEntry[] ens = (MappedEntry[])Array.newInstance(MappedEntry.getEntryClass(typ), arr.size());
				int cnt = 0;
				for (int i = 0; i < ens.length; i++) {
					try {
						ens[i] = convertEntry(typ, arr.getJSONObject(i), false);
						if (ens[i] != null)
							cnt++;
					} catch (RuntimeException ex) {
						System.out.println("Not a valid entry: " + String.valueOf(arr.get(i)));
					}
				}
				System.out.printf("%d/%d %s entries added.%n", cnt, ens.length, typ);
				return ens;
			} catch (RuntimeException ex) {
				System.out.println("Not valid entries: " + String.valueOf(cont.get(n)));
				return EntryPort.forNullEntry(ArtistEntry[].class);
			}
		}
		
		static void processCard(JSONObject cont) {
			if (!cont.containsKey("groupKey"))
				return;
			try {
				String n = cont.getString("groupKey");
				JSONArray arr = cont.getJSONArray("cards");
				if (arr.size() > 1)
					System.err.println("Multiple cards: " + arr.size());
				switch (n) {
				case "ARTIST_SIMILARIES":
					processSimilars("artist", arr.getJSONObject(0));
					break;
				case "ARTIST_ALBUMS":
					processSimilars("album", arr.getJSONObject(0));
					break;
				case "ARTIST_SONGS":
				case "ARTIST_DEMOS":
					processSimilars("song", arr.getJSONObject(0));
					break;
				case "ARTIST_MVS":
					break;
				default:
					System.out.println("Not a valid group name: " + n);
				}
			} catch (RuntimeException ex) {
				System.out.printf("Not a valid card: %s, %s%n",
					String.valueOf(cont.get("groupKey")), String.valueOf(cont.get("cards")));
			}
		}
		
		public static ArtistEntry convertArtistEntry(JSONObject o, boolean ext) {
			JSONObject cont = ext ? o.getJSONObject("artistDetail") : o;
			if (cont == null || cont.isEmpty())
				return null;
			ArtistEntry en = processEntry(cont, ext);
			if (en == null)
				return null;
			en.name = Helper.parseValidString(cont, "artistName");
			if (ext)
				System.out.println("Processing " + en.name + "...");
			en.subName = Helper.parseValidString(cont, "alias");
			if (en.subName != null && en.subName.isEmpty())
				en.subName = EntryPort.NULL_STRING;
			en.logoURL = Helper.parseValidString(cont, "artistLogo");
			en.gender = processGender(cont);
			en.birthday = Helper.parseValidInteger(cont, "birthday");
			en.area = Helper.parseValidString(cont, "area");
			if (en.area != null && en.area.isEmpty())
				en.area = null;
			en.category = AlbumConverter.processCategory(cont);
			en.playCount = Helper.parseValidInteger(cont, "playCount");
			if (en.playCount == null)
				en.playCount = EntryPort.NULL_INTEGER;
			en.likeCount = Helper.parseValidInteger(cont, "countLikes");
			if (en.likeCount == null)
				en.likeCount = EntryPort.NULL_INTEGER;
			if (!ext)
				return en;
			en.update = Helper.parseValidInteger(o, "update");
			en.styles = SongConverter.processStyles(cont);
			if (en.styles == null)
				en.styles = new StyleEntry[0];
			en.info = Helper.parseValidString(cont, "description");
			if (en.info != null && en.info.isEmpty())
				en.info = null;
			en.commentCount = Helper.parseValidInteger(cont, "comments");
			if (en.commentCount == null)
				en.commentCount = EntryPort.NULL_INTEGER;
			JSONArray arr = o.getJSONArray("artistExt");
			if (arr != null && !arr.isEmpty()) {
				for (int i = 0, len = arr.size(); i < len; i++) {
					cont = arr.getJSONObject(i);
					if (cont != null && !cont.isEmpty())
						processCard(cont);
				}
			}
			return en;
		}
		
		@Deprecated
		private ArtistConverter() {
			throw new IllegalStateException();
		}
		
	}
	
	public static final class AlbumConverter {
		
		static AlbumEntry processEntry(JSONObject cont, boolean ext) {
			Long id = cont.getLong("albumId");
			String sid = cont.getString("albumStringId");
			AlbumEntry en = null;
			if (ext) {
				en = AlbumEntry.getExactEntry(id, sid);
				if (en == null)
					en = new AlbumEntry(id, sid);
			} else {
				en = AlbumEntry.matchDummyEntry(id, sid);
			}
			return en;
		}
		
		static ReferenceEntry[] processArtists(JSONObject cont) {
			if (!cont.containsKey("artists"))
				return null;
			try {
				JSONArray arr = cont.getJSONArray("artists");
				ReferenceEntry[] ens = new ReferenceEntry[arr.size()];
				for (int i = 0; i < ens.length; i++) {
					try {
						ens[i] = Helper.parseValidEntry(arr.getJSONObject(i),
							"artistId", "artistStringId", "artistName");
					} catch (RuntimeException ex) {
						System.out.println("Not a valid artist: " + String.valueOf(arr.get(i)));
					}
				}
				return ens;
			} catch (RuntimeException ex) {
				System.out.println("Not valid artists: " + String.valueOf(cont.get("artists")));
				return EntryPort.forNullEntry(ReferenceEntry[].class);
			}
		}
		
		static ReferenceEntry[] processCompanies(JSONObject cont) {
			if (!cont.containsKey("companies"))
				return null;
			try {
				JSONArray arr = cont.getJSONArray("companies");
				ReferenceEntry[] ens = new ReferenceEntry[arr.size()];
				for (int i = 0; i < ens.length; i++) {
					try {
						ens[i] = Helper.parseValidEntry(arr.getJSONObject(i), "id", null, "name");
					} catch (RuntimeException ex) {
						System.out.println("Not a valid company: " + String.valueOf(arr.get(i)));
					}
				}
				return ens;
			} catch (RuntimeException ex) {
				System.out.println("Not valid companies: " + String.valueOf(cont.get("companies")));
				return EntryPort.forNullEntry(ReferenceEntry[].class);
			}
		}
		
		static CategoryEntry processCategory(JSONObject cont) {
			try {
				Long id = cont.getLong("categoryId");
				String n = Helper.parseValidString(cont, "albumCategory");
				CategoryEntry en = CategoryEntry.getCategory(id, n != EntryPort.NULL_STRING ? n : null);
				if (id != null && id != 0 && en == null)
					System.out.printf("No matching category for %d, %s.%n", id, n);
				return en;
			} catch (RuntimeException ex) {
				System.out.printf("Not a valid category: %s, %s%n",
					String.valueOf(cont.get("categoryId")), String.valueOf(cont.get("albumCategory")));
				return null;
			}
		}
		
		static ReferenceEntry[] processSongs(JSONObject cont) {
			if (!cont.containsKey("songs"))
				return null;
			try {
				JSONArray arr = cont.getJSONArray("songs");
				ReferenceEntry[] ens = new ReferenceEntry[arr.size()];
				int cnt = 0;
				for (int i = 0; i < ens.length; i++) {
					try {
						JSONObject o = arr.getJSONObject(i);
						ens[i] = Helper.parseValidEntry(o, "songId", "songStringId", "songName");
						if (convertEntry("song", o, false) != null)
							cnt++;
					} catch (RuntimeException ex) {
						System.out.println("Not a valid song: " + String.valueOf(arr.get(i)));
					}
				}
				System.out.printf("%d/%d songs listed.%n", cnt, ens.length);
				return ens;
			} catch (RuntimeException ex) {
				System.out.println("Not valid songs: " + String.valueOf(cont.get("songs")));
				return EntryPort.forNullEntry(ReferenceEntry[].class);
			}
		}
		
		static AlbumEntry[] processSimilars(JSONObject cont) {
			if (!cont.containsKey("albums"))
				return null;
			try {
				JSONArray arr = cont.getJSONArray("albums");
				AlbumEntry[] ens = new AlbumEntry[arr.size()];
				int cnt = 0;
				for (int i = 0; i < ens.length; i++) {
					try {
						ens[i] = (AlbumEntry)convertEntry("album", arr.getJSONObject(i), false);
						if (ens[i] != null)
							cnt++;
					} catch (RuntimeException ex) {
						System.out.println("Not a valid album: " + String.valueOf(arr.get(i)));
					}
				}
				System.out.printf("%d/%d albums added.%n", cnt, ens.length);
				return ens;
			} catch (RuntimeException ex) {
				System.out.println("Not valid albums: " + String.valueOf(cont.get("albums")));
				return EntryPort.forNullEntry(AlbumEntry[].class);
			}
		}
		
		public static AlbumEntry convertAlbumEntry(JSONObject o, boolean ext) {
			JSONObject cont = ext ? o.getJSONObject("albumDetail") : o;
			if (cont == null || cont.isEmpty())
				return null;
			AlbumEntry en = processEntry(cont, ext);
			if (en == null)
				return null;
			en.name = Helper.parseValidString(cont, "albumName");
			if (ext)
				System.out.println("Processing " + en.name + "...");
			en.subName = Helper.parseValidString(cont, "subName");
			if (en.subName != null && en.subName.isEmpty())
				en.subName = EntryPort.NULL_STRING;
			en.logoURL = Helper.parseValidString(cont, "albumLogo");
			en.artists = processArtists(cont);
			en.companies = processCompanies(cont);
			if (en.companies == null)
				en.companies = new ReferenceEntry[0];
			en.category = processCategory(cont);
			en.discCount = Helper.parseValidInteger(cont, "cdCount");
			if (en.discCount != null && en.discCount == 0)
				en.discCount = null;
			en.songCount = Helper.parseValidInteger(cont, "songCount");
			en.publishTime = Helper.parseValidInteger(cont, "gmtPublish");
			if (en.publishTime != null && en.publishTime == 0)
				en.publishTime = null;
			en.language = Helper.parseValidString(cont, "language");
			if (en.language != null && en.language.isEmpty())
				en.language = null;
			en.gradeCount = Helper.parseValidInteger(cont, "gradeCount");
			Double d = Helper.parseValidFloat(cont, "grade");
			if (d != null && d > 0.0) {
				en.grade = Math.round(d.doubleValue() * 10);
			} else if (d != null) {
				en.grade = EntryPort.NULL_INTEGER;
			}
			en.playCount = Helper.parseValidInteger(cont, "playCount");
			if (en.playCount == null)
				en.playCount = EntryPort.NULL_INTEGER;
			en.likeCount = Helper.parseValidInteger(cont, "collects");
			if (en.likeCount == null)
				en.likeCount = EntryPort.NULL_INTEGER;
			if (!ext)
				return en;
			en.update = Helper.parseValidInteger(o, "update");
			en.styles = SongConverter.processStyles(cont);
			en.info = Helper.parseValidString(cont, "description");
			if (en.info != null && en.info.isEmpty())
				en.info = null;
			en.songs = processSongs(cont);
			en.commentCount = Helper.parseValidInteger(cont, "comments");
			if (en.commentCount == null)
				en.commentCount = EntryPort.NULL_INTEGER;
			cont = o.getJSONObject("artistAlbums");
			if (cont != null && !cont.isEmpty())
				processSimilars(cont);
			return en;
		}
		
		@Deprecated
		private AlbumConverter() {
			throw new IllegalStateException();
		}
		
	}
	
	public static final class SongConverter {
		
		static SongEntry processEntry(JSONObject cont, boolean ext) {
			Long id = cont.getLong("songId");
			String sid = cont.getString("songStringId");
			SongEntry en = null;
			if (ext) {
				en = SongEntry.getExactEntry(id, sid);
				if (en == null)
					en = new SongEntry(id, sid);
			} else {
				en = SongEntry.matchDummyEntry(id, sid);
			}
			return en;
		}
		
		static ReferenceEntry processArtist(JSONObject cont) {
			JSONArray arr = cont.getJSONArray("artistVOs");
			if (arr == null || arr.isEmpty())
				return null;
			if (arr.size() > 1)
				System.err.println("Multiple artists: " + arr.size());
			return Helper.parseValidEntry(arr.getJSONObject(0),
				"artistId", "artistStringId", "artistName");
		}
		
		private static StaffEntry processStaff(JSONObject cont) {
			StaffEntry en = new StaffEntry(cont.getString("type"));
			en.name = Helper.parseValidString(cont, "name");
			try {
				JSONArray arr = cont.getJSONArray("staffs");
				en.artists = new ReferenceEntry[arr.size()];
				for (int i = 0; i < en.artists.length; i++) {
					JSONObject o = arr.getJSONObject(i);
					en.artists[i] = Helper.parseValidEntry(o, "id", null, "name");
				}
			} catch (RuntimeException ex) {
				System.out.println("Not valid staff: " + String.valueOf(cont.get("staffs")));
				en.artists = EntryPort.forNullEntry(ReferenceEntry[].class);
			}
			return en;
		}
		
		static StaffEntry[] processStaffs(JSONObject cont) {
			if (!cont.containsKey("behindStaffs"))
				return null;
			try {
				JSONArray arr = cont.getJSONArray("behindStaffs");
				StaffEntry[] ens = new StaffEntry[arr.size()];
				for (int i = 0; i < ens.length; i++) {
					try {
						ens[i] = processStaff(arr.getJSONObject(i));
					} catch (RuntimeException ex) {
						System.out.println("Not valid staff: " + String.valueOf(arr.get(i)));
					}
				}
				return ens;
			} catch (RuntimeException ex) {
				System.out.println("Not valid staffs: " + String.valueOf(cont.get("behindStaffs")));
				return EntryPort.forNullEntry(StaffEntry[].class);
			}
		}
		
		static StaffEntry processSingers(JSONObject cont) {
			if (!cont.containsKey("singerVOs"))
				return null;
			StaffEntry en = new StaffEntry("SINGER");
			en.name = "演唱";
			try {
				JSONArray arr = cont.getJSONArray("singerVOs");
				en.artists = new ReferenceEntry[arr.size()];
				for (int i = 0; i < en.artists.length; i++) {
					try {
						en.artists[i] = Helper.parseValidEntry(arr.getJSONObject(i),
							"artistId", "artistStringId", "artistName");
					} catch (RuntimeException ex) {
						System.out.println("Not a valid singer: " + String.valueOf(arr.get(i)));
					}
				}
			} catch (RuntimeException ex) {
				System.out.println("Not valid singers: " + String.valueOf(cont.get("singerVOs")));
				en.artists = EntryPort.forNullEntry(ReferenceEntry[].class);
			}
			return en;
		}
		
		static StyleEntry[] processStyles(JSONObject cont) {
			if (!cont.containsKey("styles"))
				return null;
			try {
				JSONArray arr = cont.getJSONArray("styles");
				StyleEntry[] ens = new StyleEntry[arr.size()];
				for (int i = 0; i < ens.length; i++) {
					try {
						JSONObject o = arr.getJSONObject(i);
						Long id = o.getLong("styleId");
						String n = Helper.parseValidString(o, "styleName");
						ens[i] = StyleEntry.getStyle(id, n != EntryPort.NULL_STRING ? n : null);
						if (id != null && id != 0 && ens[i] == null)
							System.out.printf("No matching style for %d, %s.%n", id, n);
					} catch (RuntimeException ex) {
						System.out.println("Not a valid style: " + String.valueOf(arr.get(i)));
					}
				}
				return ens;
			} catch (RuntimeException ex) {
				System.out.println("Not valid styles: " + String.valueOf(cont.get("styles")));
				return EntryPort.forNullEntry(StyleEntry[].class);
			}
		}
		
		static String[][] processTags(JSONObject cont) {
			if (!cont.containsKey("songTag"))
				return null;
			try {
				cont = cont.getJSONObject("songTag");
				if (!cont.containsKey("tags"))
					return new String[0][];
				JSONArray arr = cont.getJSONArray("tags");
				String[][] ens = new String[arr.size()][];
				for (int i = 0; i < ens.length; i++) {
					try {
						JSONObject o = arr.getJSONObject(i);
						ens[i] = new String[] {o.getString("id"), o.getString("name")};
					} catch (RuntimeException ex) {
						System.out.println("Not a valid tag: " + String.valueOf(arr.get(i)));
					}
				}
				return ens;
			} catch (RuntimeException ex) {
				System.out.println("Not valid tags: " + String.valueOf(cont.get("songTag")));
				return EntryPort.forNullEntry(String[][].class);
			}
		}
		
		static InfoEntry[] processInfos(JSONObject cont) {
			if (!cont.containsKey("songDescs"))
				return null;
			try {
				JSONArray arr = cont.getJSONArray("songDescs");
				InfoEntry[] ens = new InfoEntry[arr.size()];
				for (int i = 0; i < ens.length; i++) {
					try {
						JSONObject o = arr.getJSONObject(i);
						ens[i] = new InfoEntry();
						ens[i].title = Helper.parseValidString(o, "title");
						ens[i].content = Helper.parseValidString(o, "desc");
					} catch (RuntimeException ex) {
						System.out.println("Not valid info: " + String.valueOf(arr.get(i)));
					}
				}
				return ens;
			} catch (RuntimeException ex) {
				System.out.println("Not valid infos: " + String.valueOf(cont.get("songDescs")));
				return EntryPort.forNullEntry(InfoEntry[].class);
			}
		}
		
		static SongEntry[] processSimilars(JSONObject cont) {
			String n = "similarSongsFullInfo";
			if (!cont.containsKey(n))
				return null;
			try {
				JSONArray arr = cont.getJSONArray(n);
				SongEntry[] ens = new SongEntry[arr.size()];
				int cnt = 0;
				for (int i = 0; i < ens.length; i++) {
					try {
						ens[i] = (SongEntry)convertEntry("song", arr.getJSONObject(i), false);
						if (ens[i] != null)
							cnt++;
					} catch (RuntimeException ex) {
						System.out.println("Not a valid song: " + String.valueOf(arr.get(i)));
					}
				}
				System.out.printf("%d/%d songs added.%n", cnt, ens.length);
				return ens;
			} catch (RuntimeException ex) {
				System.out.println("Not valid songs: " + String.valueOf(cont.get(n)));
				return EntryPort.forNullEntry(SongEntry[].class);
			}
		}
		
		static LyricEntry[] processLyrics(JSONObject cont) {
			if (!cont.containsKey("songLyric"))
				return null;
			try {
				JSONArray arr = cont.getJSONArray("songLyric");
				LyricEntry[] ens = new LyricEntry[arr.size()];
				for (int i = 0; i < ens.length; i++) {
					try {
						JSONObject o = arr.getJSONObject(i);
						ens[i] = new LyricEntry(o.getLong("id"));
						ens[i].update = Helper.parseValidInteger(o, "gmtModified");
						ens[i].type = Helper.parseValidInteger(o, "type");
						ens[i].official = Helper.parseValidBoolean(o, "flagOfficial");
						ens[i].contentURL = Helper.parseValidString(o, "lyricUrl");
					} catch (RuntimeException ex) {
						System.out.println("Not a valid lyric: " + String.valueOf(arr.get(i)));
					}
				}
				return ens;
			} catch (RuntimeException ex) {
				System.out.println("Not valid lyrics: " + String.valueOf(cont.get("songLyric")));
				return EntryPort.forNullEntry(LyricEntry[].class);
			}
		}
		
		public static SongEntry convertSongEntry(JSONObject o, boolean ext) {
			JSONObject cont = ext ? o.getJSONObject("songDetail") : o;
			if (cont == null || cont.isEmpty())
				return null;
			SongEntry en = processEntry(cont, ext);
			if (en == null)
				return null;
			en.name = Helper.parseValidString(cont, "songName");
			if (ext)
				System.out.println("Processing " + en.name + "...");
			en.subName = Helper.parseValidString(cont, "newSubName");
			if (en.subName == null || en.subName == EntryPort.NULL_STRING || en.subName.isEmpty())
				en.subName = Helper.parseValidString(cont, "subName");
			if (en.subName != null && en.subName.isEmpty())
				en.subName = EntryPort.NULL_STRING;
			en.translation = Helper.parseValidString(cont, "translation");
			if (en.translation != null && en.translation.isEmpty())
				en.translation = EntryPort.NULL_STRING;
			en.artist = processArtist(cont);
			en.album = Helper.parseValidEntry(cont, "albumId", "albumStringId", "albumName");
			en.disc = Helper.parseValidInteger(cont, "cdSerial");
			if (en.disc != null && en.disc == 0)
				en.disc = null;
			en.track = Helper.parseValidInteger(cont, "track");
			if (en.track != null && en.track == 0)
				en.track = EntryPort.NULL_INTEGER;
			en.length = Helper.parseValidInteger(cont, "length");
			if (en.length != null && en.length == 0)
				en.length = EntryPort.NULL_INTEGER;
			en.pace = Helper.parseValidInteger(cont, "pace");
			if (en.pace != null && en.pace == 0)
				en.pace = null;
			en.highlightOffset = Helper.parseValidInteger(cont, "hotPartStartTime");
			if (en.highlightOffset != null && en.highlightOffset != EntryPort.NULL_INTEGER) {
				Long t = Helper.parseValidInteger(cont, "hotPartEndTime");
				if (t != null && t != EntryPort.NULL_INTEGER)
					t = (t > en.highlightOffset) ? t - en.highlightOffset : EntryPort.NULL_INTEGER;
				if (en.highlightOffset == 0 && (t == null || t == EntryPort.NULL_INTEGER)) {
					en.highlightOffset = null;
				} else {
					en.highlightLength = t;
				}
			}
			en.playCount = Helper.parseValidInteger(cont, "playCount");
			if (en.playCount == null)
				en.playCount = EntryPort.NULL_INTEGER;
			en.likeCount = Helper.parseValidInteger(cont, "favCount");
			if (en.likeCount == null)
				en.likeCount = EntryPort.NULL_INTEGER;
			if (!ext)
				return en;
			en.update = Helper.parseValidInteger(o, "update");
			cont = o.getJSONObject("songExt");
			if (cont != null && !cont.isEmpty()) {
				en.staffs = processStaffs(cont);
				StaffEntry ens = processSingers(cont);
				if (ens != null) {
					en.staffs = (en.staffs != null) ?
						Arrays.copyOf(en.staffs, en.staffs.length + 1) : new StaffEntry[1];
					en.staffs[en.staffs.length - 1] = ens;
				}
				try {
					if (convertEntry("album", cont.getJSONObject("album"), false) != null)
						System.out.println("Album extension added.");
				} catch (RuntimeException ex) {
					System.out.println("Not a valid album: " + String.valueOf(cont.get("album")));
				}
				en.styles = processStyles(cont);
				en.tags = processTags(cont);
				en.infos = processInfos(cont);
				if (Helper.isEmptyArray(en.infos))
					en.infos = null;
				en.commentCount = Helper.parseValidInteger(cont, "commentCount");
				if (en.commentCount == null)
					en.commentCount = EntryPort.NULL_INTEGER;
				processSimilars(cont);
			}
			en.lyrics = processLyrics(o);
			return en;
		}
		
		@Deprecated
		private SongConverter() {
			throw new IllegalStateException();
		}
		
	}
	
	public static MappedEntry convertEntry(String typ, JSONObject o, boolean ext) {
		switch (typ) {
		case "artist":
			return ArtistConverter.convertArtistEntry(o, ext);
		case "album":
			return AlbumConverter.convertAlbumEntry(o, ext);
		case "song":
			return SongConverter.convertSongEntry(o, ext);
		default:
			throw new IllegalArgumentException("Unknown entry type.");
		}
	}
	public static MappedEntry convertEntry(String typ, String dat) {
		return convertEntry(typ, JSON.parseObject(dat), true);
	}
	
	public static List<MappedEntry> convertJSONM(String typ, File f) throws IOException {
		if (MappedEntry.getEntryClass(typ) == null)
			throw new IllegalArgumentException("Unknown entry type.");
		InputStream in = new FileInputStream(f);
		BufferedReader rdr = new BufferedReader(new InputStreamReader(in, "UTF-8"));
		List<MappedEntry> li = new ArrayList<>();
		try {
			System.out.println("Converting " + f.getName() + "...");
			String ln = null;
			while ((ln = rdr.readLine()) != null) {
				MappedEntry en = null;
				try {
					en = convertEntry(typ, ln);
				} catch (RuntimeException ex) {
					System.err.println("Unexpected break:");
					ex.printStackTrace();
				}
				if (en != null) {
					System.out.println("Accepted: " + en.name);
					li.add(en);
				} else {
					System.out.println("Rejected: " + ln);
				}
			}
			System.out.println("Convert complete.");
			return li;
		} catch (Exception ex) {
			System.err.println("Convert failed.");
			throw ex;
		} finally {
			rdr.close();
			System.gc();
		}
	}
	
	@Deprecated
	private Converter() {
		throw new IllegalStateException();
	}
	
}
