import * as admin from 'firebase-admin';
import { onCall } from 'firebase-functions/v2/https';
import { defineSecret } from 'firebase-functions/params';
import { Timestamp } from 'firebase-admin/firestore';

admin.initializeApp();

const googleBooksKey = defineSecret('GOOGLE_BOOKS_API_KEY');
const openRouterKey = defineSecret('OPENROUTER_API_KEY');

const DEFAULT_TTL_MS = 3650 * 24 * 60 * 60 * 1000; // 10 años: cache indefinida

interface ResolveBookInput {
  isbn?: string;
  titulo?: string;
  autor?: string;
  uid?: string;
  force?: boolean;
}

interface CloudMetadata {
  coverUrl?: string | null;
  description?: string | null;
  averageRating?: number | null;
  ratingsCount?: number | null;
  ratingSource?: string | null;
  characters?: { name: string; isFavorite: boolean; emoji?: string | null }[] | null;
}

interface CacheDoc {
  isbn: string;
  coverUrl: string | null;
  description: string | null;
  averageRating: number | null;
  ratingsCount: number | null;
  ratingSource: string | null;
  characters: { name: string; isFavorite: boolean; emoji?: string | null }[] | null;
  updatedAt: admin.firestore.Timestamp;
  ttlMs: number;
}

function cleanIsbn(isbn?: string): string {
  return (isbn ?? '').replace(/[\s-]/g, '').trim();
}

async function fetchJson(url: string, headers: Record<string, string> = {}): Promise<any> {
  const controller = new AbortController();
  const timer = setTimeout(() => controller.abort(), 12_000);
  try {
    const res = await fetch(url, {
      headers: { 'User-Agent': 'Alexandria/1.0 (Android Book Tracker)', ...headers },
      signal: controller.signal,
    });
    if (!res.ok) throw new Error(`HTTP ${res.status} for ${url}`);
    return await res.json();
  } finally {
    clearTimeout(timer);
  }
}

function mergeMetadata(primary: CloudMetadata, fallback: CloudMetadata): CloudMetadata {
  return {
    coverUrl: primary.coverUrl ?? fallback.coverUrl ?? null,
    description: primary.description ?? fallback.description ?? null,
    averageRating: primary.averageRating ?? fallback.averageRating ?? null,
    ratingsCount: primary.ratingsCount ?? fallback.ratingsCount ?? null,
    ratingSource: primary.ratingSource ?? fallback.ratingSource ?? null,
    characters: primary.characters ?? fallback.characters ?? null,
  };
}

function isEmptyMeta(m: CloudMetadata): boolean {
  return !m.coverUrl && !m.description && !(m.characters && m.characters.length > 0);
}

async function openLibraryByIsbn(isbn: string): Promise<CloudMetadata> {
  const url = `https://openlibrary.org/api/books?bibkeys=ISBN:${isbn}&jscmd=data&format=json`;
  const data = await fetchJson(url);
  const entry = data[`ISBN:${isbn}`];
  if (!entry) return {};
  return {
    coverUrl: entry.cover?.large ?? entry.cover?.medium ?? entry.cover?.small ?? null,
    description: typeof entry.description === 'string' ? entry.description : entry.description?.value ?? null,
    averageRating: null,
    ratingsCount: null,
    ratingSource: 'openlibrary',
  };
}

async function openLibraryByTitle(titulo: string, autor?: string): Promise<CloudMetadata> {
  const q = [titulo, autor].filter(Boolean).join(' ').trim();
  const url = `https://openlibrary.org/search.json?q=${encodeURIComponent(q)}&fields=key,title,author_name,cover_i,isbn&limit=3`;
  const data = await fetchJson(url);
  const docs = (data.docs ?? []) as any[];
  const picked = docs.find((d) => d.cover_i) ?? docs[0];
  if (!picked) return {};
  let result: CloudMetadata = {
    coverUrl: picked.cover_i ? `https://covers.openlibrary.org/b/id/${picked.cover_i}-L.jpg` : null,
    ratingSource: 'openlibrary',
  };
  const isbn = (picked.isbn ?? []).find((i: string) => /^[0-9]{10,13}$/.test(i));
  if (isbn) {
    const byIsbn = await openLibraryByIsbn(isbn).catch(() => ({} as CloudMetadata));
    result = mergeMetadata(byIsbn, result);
  }
  if (!result.description && picked.key) {
    try {
      const work = await fetchJson(`https://openlibrary.org${picked.key}.json`);
      const desc = work.description;
      result.description = typeof desc === 'string' ? desc : desc?.value ?? null;
    } catch (e) {
      console.warn('openlibrary work desc failed', e);
    }
  }
  return result;
}

async function googleBooksByIsbn(isbn: string): Promise<CloudMetadata> {
  const key = googleBooksKey.value();
  const url = `https://www.googleapis.com/books/v1/volumes?q=isbn:${isbn}&langRestrict=es${key ? `&key=${key}` : ''}`;
  const data = await fetchJson(url);
  const item = data.items?.[0];
  if (!item) return {};
  const vi = item.volumeInfo ?? {};
  return {
    coverUrl: vi.imageLinks?.large ?? vi.imageLinks?.thumbnail ?? null,
    description: vi.description ?? null,
    averageRating: vi.averageRating ?? null,
    ratingsCount: vi.ratingsCount ?? null,
    ratingSource: 'google_books',
  };
}

async function googleBooksByTitle(titulo: string, autor?: string): Promise<CloudMetadata> {
  const key = googleBooksKey.value();
  const q = `intitle:${titulo}${autor ? `+inauthor:${autor}` : ''}`;
  const url = `https://www.googleapis.com/books/v1/volumes?q=${encodeURIComponent(q)}&langRestrict=es&maxResults=3${key ? `&key=${key}` : ''}`;
  const data = await fetchJson(url);
  const picked = (data.items ?? []).find((it: any) => it.volumeInfo?.imageLinks) ?? data.items?.[0];
  if (!picked) return {};
  const vi = picked.volumeInfo ?? {};
  return {
    coverUrl: vi.imageLinks?.large ?? vi.imageLinks?.thumbnail ?? null,
    description: vi.description ?? null,
    averageRating: vi.averageRating ?? null,
    ratingsCount: vi.ratingsCount ?? null,
    ratingSource: 'google_books',
  };
}

async function bneByIsbn(isbn: string): Promise<CloudMetadata> {
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
  if (!row) return {};
  return {
    coverUrl: null,
    description: row.abstract?.value ?? null,
    averageRating: null,
    ratingsCount: null,
    ratingSource: 'bne',
  };
}

async function openRouterResolve(input: ResolveBookInput): Promise<CloudMetadata> {
  const apiKey = openRouterKey.value();
  if (!apiKey) return {};
  const modelList = (process.env.OPENROUTER_MODELS ?? 'google/gemma-4-26b-a4b-it:free,openai/gpt-oss-20b:free')
    .split(',')
    .map((m) => m.trim())
    .filter(Boolean);
  const prompt = `Dame información de este libro.
Título: ${input.titulo}
Autor: ${input.autor ?? ''}
${input.isbn ? `ISBN: ${input.isbn}` : ''}

Responde SOLO con JSON válido (sin markdown, sin texto extra) con este esquema exacto:
{"description":"sinopsis en español de 120-180 palabras","characters":[{"name":"Nombre","isFavorite":false,"emoji":"😀"}],"rating":null}
Si no conoces el libro, responde {"description":null,"characters":[]}.
Los personajes deben ser como máximo 10, solo personajes reales del libro (no inventes). Cada personaje debe incluir un emoji representativo de su rol o personalidad.`;

  const system = { role: 'system', content: 'Responde SOLO con JSON válido, sin markdown ni texto extra.' };
  for (const model of modelList) {
    try {
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
          messages: [system, { role: 'user', content: prompt }],
          temperature: 0.4,
          max_tokens: 800,
          response_format: { type: 'json_object' },
        }),
      });
      if (!res.ok) {
        console.warn(`openrouter ${model} HTTP ${res.status}; probando siguiente modelo`);
        continue;
      }
      const data = (await res.json()) as { choices?: { message?: { content?: string } }[] };
      const text = data.choices?.[0]?.message?.content ?? '';
      const cleaned = text.replace(/```json|```/g, '').trim();
      let parsed: any;
      try {
        parsed = JSON.parse(cleaned);
      } catch {
        console.warn(`openrouter ${model} respuesta no-json; probando siguiente modelo`);
        continue;
      }
      if (!parsed || (typeof parsed.description !== 'string' && !Array.isArray(parsed.characters))) {
        continue;
      }
      return {
        description: typeof parsed.description === 'string' ? parsed.description : null,
        averageRating: null,
        ratingsCount: null,
        ratingSource: 'openrouter',
        characters: Array.isArray(parsed.characters)
          ? parsed.characters
              .filter((c: any) => typeof c?.name === 'string' && c.name.trim().length > 0)
              .slice(0, 10)
              .map((c: any) => ({
                name: c.name.trim(),
                isFavorite: false,
                emoji: typeof c?.emoji === 'string' ? c.emoji.trim().slice(0, 4) : null,
              }))
          : null,
      };
    } catch (e) {
      console.warn(`openrouter ${model} failed`, e);
    }
  }
  return {};
}

async function resolveBookChain(input: ResolveBookInput): Promise<CloudMetadata> {
  const isbn = cleanIsbn(input.isbn);
  let result: CloudMetadata = {};

  if (isbn) {
    for (const provider of [openLibraryByIsbn, googleBooksByIsbn, bneByIsbn]) {
      try {
        const m = await provider(isbn);
        result = mergeMetadata(result, m);
        if (result.coverUrl && result.description) break;
      } catch (e) {
        console.warn(`${provider.name} failed:`, e);
      }
    }
  }

  if (isEmptyMeta(result) && input.titulo) {
    for (const provider of [openLibraryByTitle, googleBooksByTitle]) {
      try {
        const m = await provider(input.titulo, input.autor);
        result = mergeMetadata(result, m);
        if (result.coverUrl && result.description) break;
      } catch (e) {
        console.warn(`${provider.name} (title) failed:`, e);
      }
    }
  }

  if (!result.description || !(result.characters && result.characters.length > 0)) {
    const ia = await openRouterResolve(input);
    result = mergeMetadata(result, ia);
  }

  return result;
}

export const resolveBook = onCall({ enforceAppCheck: false, secrets: [googleBooksKey, openRouterKey] }, async (request) => {
  console.log('resolveBook invoked: enforceAppCheck=false build');
  const input = (request.data ?? {}) as ResolveBookInput;
  const isbn = cleanIsbn(input.isbn);
  if (!isbn && !input.titulo) {
    throw new Error('isbn o titulo requeridos');
  }
  const docKey = isbn || `t-${input.titulo!.toLowerCase().replace(/\s+/g, '-').slice(0, 80)}`;
  const ref = admin.firestore().doc(`metadata/${docKey}`);
  const doc = await ref.get();
  if (doc.exists && !input.force) {
    const data = doc.data() as CacheDoc;
    if (data && data.updatedAt) {
      return { ...data, characters: data.characters ?? [] };
    }
  }

  const result = await resolveBookChain(input);
  if (isEmptyMeta(result)) {
    return { coverUrl: null, description: null, averageRating: null, ratingsCount: null, ratingSource: null, characters: [] };
  }

  const payload: CacheDoc = {
    isbn,
    coverUrl: result.coverUrl ?? null,
    description: result.description ?? null,
    averageRating: result.averageRating ?? null,
    ratingsCount: result.ratingsCount ?? null,
    ratingSource: result.ratingSource ?? null,
    characters: result.characters ?? null,
    updatedAt: Timestamp.now(),
    ttlMs: DEFAULT_TTL_MS,
  };
  const wasNew = !doc.exists;
  await ref.set(payload, { merge: true });

  if (input.uid && wasNew) {
    try {
      await admin.messaging().send({
        topic: `user-${input.uid}`,
        notification: {
          title: 'Alexandria',
          body: `Metadatos listos para "${input.titulo ?? isbn}"`,
        },
        data: { type: 'metadata_ready', isbn, title: input.titulo ?? '', body: `Metadatos listos para "${input.titulo ?? isbn}"` },
      });
    } catch (e) {
      console.warn('fcm push failed', e);
    }
  }

  return { ...result, characters: result.characters ?? [] };
});
