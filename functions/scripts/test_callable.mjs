// End-to-end test of the resolveBook callable against the local Functions emulator.
// Run: firebase emulators:exec --only functions,firestore "node functions/scripts/test_callable.mjs"
const base = 'http://127.0.0.1:5001/alexandria-d3397/us-central1';

async function callResolveBook(payload) {
  const res = await fetch(`${base}/resolveBook`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ data: payload }),
  });
  const text = await res.text();
  console.log(`[${payload.titulo ?? payload.isbn}] status=${res.status}`);
  try {
    const json = JSON.parse(text);
    const r = json.result;
    if (r) {
      console.log(`  coverUrl=${!!r.coverUrl} desc=${r.description ? r.description.length + ' chars' : 'null'} rating=${r.averageRating ?? 'null'} chars=${(r.characters ?? []).length}`);
      console.log(`  source=${r.ratingSource}`);
    } else {
      console.log('  ', JSON.stringify(json).slice(0, 300));
    }
  } catch {
    console.log('  ', text.slice(0, 300));
  }
}

await callResolveBook({ isbn: '9788408164953', titulo: 'Cien años de soledad', autor: 'Gabriel García Márquez', uid: 'test-user' });
console.log('---');
await callResolveBook({ titulo: 'La casa de los espíritus', autor: 'Isabel Allende', uid: 'test-user' });
