load('config.js');
function execute(key, page) {
	if (!page) page = 1;
	let response = fetch(config_host + "/searchBook?key=" + key + "&page=" + page);
	if (response.ok) {
		let doc = response.json();
		let item_list = doc.data;
		if (!item_list) return Response.success([]);
		const data = [];
		item_list.forEach((e) => {
			data.push({
				name: e.name,
				link: config_host + "/getChapterList?url=" + e.bookUrl,
				cover: e.coverUrl,
				description: e.author,
				host: config_host
			})
		});
		return Response.success(data)
	}
	return null;
}