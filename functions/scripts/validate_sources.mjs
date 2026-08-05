#!/usr/bin/env node
// Validación runtime de las fuentes BNE (SPARQL) y OpenRouter (fallback IA).
// Uso: node validate_sources.mjs [--isbn 9788408164953] [--titulo "Cien anios de soledad"] [--autor "Gabriel Garcia Marquez"]
//
// Necesita:  OPENROUTER_API_KEY  (opcional; si falta, salta la prueba OpenRouter)
// Lanza:     node validate_sources.mjs

const ISBN_TEST = '9788408164953';
const TITULO_TEST = 'Cien años de soledad';
const AUTOR_TEST = 'Gabriel García Márquez';

function argv(key, def) {
  const i = process.argv.indexOf('--' + key);
  return i > -1 && process.argv[i + 1] ? process.argv[i + 1] : def;
}

async function fetchJson(url, headers = {}) {
  const res = await fetch(url, { headers: { 'User-Agent': 'Alexandria/1.0 (Android Book Tracker)', ...headers } });
  if (!res.ok) throw new Error(`HTTP ${res.status}`);
  return res.json();
}

async function testOpenLibrary(isbn) {
  const data = await fetchJson(`https://openlibrary.org/api/books?bibkeys=ISBN:${isbn}&jscmd=data&format=json`);
  const entry = data[`ISBN:${isbn}`];
  console.log('[OpenLibrary]', entry ? `cover=${!!entry.cover} desc=${typeof entry.description}` : 'sin datos');
  return entry ? { cover: !!entry.cover, desc: !!entry.description } : null;
}

async function testGoogleBooks(isbn) {
  const data = await fetchJson(`https://www.googleapis.com/books/v1/volumes?q=isbn:${isbn}&langRestrict=es`);
  const vi = data.items?.[0]?.volumeInfo;
  console.log('[GoogleBooks]', vi ? `desc=${!!vi.description} rating=${vi.averageRating ?? 'n/a'} cover=${!!vi.imageLinks}` : 'sin datos');
  return vi ? { desc: !!vi.description, rating: vi.averageRating ?? null } : null;
}

async function testBne(isbn) {
  const query = `
PREFIX bneont: <http://datos.bne.es/def/>
PREFIX dct: <http://purl.org/dc/terms/>
PREFIX dc: <http://purl.org/dc/elements/1.1/>
SELECT ?obra ?titulo ?abstract
WHERE {
  ?obra bneont:ISBN "${isbn}" .
  ?obra dct:title ?titulo .
  OPTIONAL { ?obra dc:description ?abstract }
}
LIMIT 1`.replace(/\s+/g, ' ');
  const url = `https://datos.bne.es/sparql?query=${encodeURIComponent(query)}&format=json`;
  const data = await fetchJson(url, { Accept: 'application/json' });
  const row = data.results?.bindings?.[0];
  console.log('[BNE SPARQL]', row ? `titulo=${row.titulo?.value} abstract=${!!row.abstract}` : 'sin resultados');
  return row ? { title: row.titulo?.value, abstract: !!row.abstract } : null;
}

async function testOpenRouter(titulo, autor) {
  const apiKey = process.env.OPENROUTER_API_KEY;
  if (!apiKey) {
    console.log('[OpenRouter] SKIP (OPENROUTER_API_KEY no definida)');
    return null;
  }
  const model = process.env.OPENROUTER_MODEL ?? 'meta-llama/llama-3.1-8b-instruct:free';
  const res = await fetch('https://openrouter.ai/api/v1/chat/completions', {
    method: 'POST',
    headers: {
      Authorization: `Bearer ${apiKey}`,
      'Content-Type': 'application/json',
      'HTTP-Referer': 'https://alexandria.app',
      'X-Title': 'Alexandria Book Tracker',
    },
    body: JSON.stringify({
      model,
      messages: [
        { role: 'system', content: 'Responde SOLO con JSON válido, sin markdown ni texto extra.' },
        { role: 'user', content: `Libro: "${titulo}" de ${autor}. Responde SOLO JSON: {"description":"sinopsis 120-180 palabras en español","characters":[{"name":"Nombre"}]} max 8 personajes reales. Si no lo conoces: {"description":null,"characters":[]}` },
      ],
      temperature: 0.4,
      max_tokens: 800,
    }),
  });
  if (!res.ok) throw new Error(`HTTP ${res.status}`);
  const data = await res.json();
  const text = (data.choices?.[0]?.message?.content ?? '').replace(/```json|```/g, '').trim();
  const parsed = JSON.parse(text);
  const chars = Array.isArray(parsed.characters) ? parsed.characters.length : 0;
  console.log(`[OpenRouter ${model}] desc=${!!parsed.description} (${parsed.description?.length ?? 0} chars) personajes=${chars}`);
  return { desc: !!parsed.description, chars };
}

async function main() {
  const isbn = argv('isbn', ISBN_TEST);
  const titulo = argv('titulo', TITULO_TEST);
  const autor = argv('autor', AUTOR_TEST);
  console.log(`== Validando fuentes para ISBN=${isbn} titulo="${titulo}" autor="${autor}" ==\n`);
  const results = {};
  try { results.openlibrary = await testOpenLibrary(isbn); } catch (e) { console.log('[OpenLibrary] ERROR', e.message); }
  try { results.googleBooks = await testGoogleBooks(isbn); } catch (e) { console.log('[GoogleBooks] ERROR', e.message); }
  try { results.bne = await testBne(isbn); } catch (e) { console.log('[BNE] ERROR', e.message); }
  try { results.openRouter = await testOpenRouter(titulo, autor); } catch (e) { console.log('[OpenRouter] ERROR', e.message); }
  console.log('\n== Resumen ==');
  console.log(JSON.stringify(results, null, 2));
  const ok = Object.values(results).some((r) => r && (r.desc || r.cover || r.abstract || r.chars));
  if (!ok) {
    console.error('\nNINGUNA fuente resolvió datos. Revisar conectividad/ontología.');
    process.exit(1);
  }
  console.log('\nOK: al menos una fuente resolvió datos.');
}

main().catch((e) => {
  console.error(e);
  process.exit(1);
});
