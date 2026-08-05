const isbn = '9788408164953';
const title = 'Cien años de soledad';

async function tryFetch(label, url, headers = {}) {
  try {
    const r = await fetch(url, { headers: { 'User-Agent': 'Alexandria/1.0 (Android Book Tracker)', ...headers } });
    const t = await r.text();
    console.log(`${label} => ${r.status} ${r.statusText} | ${t.slice(0, 120)}`);
    return { status: r.status, text: t };
  } catch (e) {
    console.log(`${label} => ERR ${e.message}`);
    return null;
  }
}

await tryFetch('OpenLibrary byISBN', `https://openlibrary.org/api/books?bibkeys=ISBN:${isbn}&jscmd=data&format=json`);
await tryFetch('OpenLibrary search', `https://openlibrary.org/search.json?q=${encodeURIComponent(title)}&fields=key,title,author_name,cover_i&limit=2`);
await tryFetch('GoogleBooks isbn', `https://www.googleapis.com/books/v1/volumes?q=isbn:${isbn}&langRestrict=es`);
await tryFetch('Wikipedia es', `https://es.wikipedia.org/w/api.php?action=query&prop=extracts&exintro&explaintext&format=json&titles=${encodeURIComponent(title)}`);
await tryFetch('Wikidata P674 sample', `https://query.wikidata.org/sparql?query=${encodeURIComponent('SELECT ?char WHERE { wd:Q1449 p:P674 ?s. ?s ps:P674 ?char } LIMIT 3')}&format=json`, { Accept: 'application/json' });
