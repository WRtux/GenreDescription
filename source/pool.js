"use strict";
var pool = { artists: new Map(), albums: new Map(), songs: new Map(), comments: new Map() };

for (let n in pool) {
	pool[n].weigh = function (o) {
		if (!o || (!o.sid && !o.id))
			return;
		let w = (this.get(o.sid || o.id) || { weight: 0 }).weight || 10;
		if (!w || o.weight >= w) {
			let en = new Object();
			Object.assign(en, o);
			this.set(o.sid || o.id, en);
		}
	};
	pool[n].inflate = function (o) {
		if (!o || (!o.sid && !o.id))
			return;
		let en = this.get(o.sid || o.id) || new Object();
		delete en.referrer;
		delete en.weight;
		Object.assign(en, o);
		this.set(o.sid || o.id, en);
	};
}

function loadPool(dat) {
	if (typeof dat == "string")
		dat = JSON.parse(dat);
	for (let n in dat) {
		pool[n].clear();
		for (let en of dat[n])
			pool[n].set(en.sid || en.id, en);
	}
}

function savePool() {
	let dat = new Object();
	for (let n in pool) {
		dat[n] = new Array();
		for (let [k, v] of pool[n])
			dat[n].push(v);
	}
	downloadJSON(dat);
}

function inflatePool() {
	//TODO
}
