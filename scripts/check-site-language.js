// Comprueba la elección de idioma de la portada (site/idioma.js) simulando el navegador:
// es, en, pt y zh, la elección manual, una sola redirección y el almacenamiento bloqueado.
// Uso: node scripts/check-site-language.js
const assert = require("node:assert/strict");
const fs = require("node:fs");
const path = require("node:path");
const vm = require("node:vm");

const code = fs.readFileSync(path.join(__dirname, "..", "site", "idioma.js"), "utf8");

function storage(blocked) {
  const data = new Map();
  if (blocked) {
    return {
      getItem() {
        throw new Error("bloqueado");
      },
      setItem() {
        throw new Error("bloqueado");
      },
    };
  }
  return { getItem: (k) => (data.has(k) ? data.get(k) : null), setItem: (k, v) => data.set(k, String(v)) };
}

/** Un navegador con su idioma y su almacenamiento; cada `open` es una página nueva. */
function browser(language, { blocked = false } = {}) {
  const local = storage(blocked);
  const session = storage(blocked);
  const requests = [];
  return {
    requests,
    open(isHome) {
      const visit = { redirectedTo: null, click: null };
      const document = {
        currentScript: null,
        addEventListener(type, handler) {
          if (type === "click") visit.click = handler;
        },
      };
      const window = {
        document,
        navigator: { language },
        localStorage: local,
        sessionStorage: session,
        location: {
          replace(url) {
            visit.redirectedTo = url;
          },
        },
        // Ninguna consulta externa: si el script intentara usar la red, quedaría registrado.
        fetch(url) {
          requests.push(url);
        },
        XMLHttpRequest: function () {
          requests.push("xhr");
        },
      };
      const sandbox = { window, document, globalThis: undefined };
      sandbox.globalThis = sandbox;
      vm.runInNewContext(code, sandbox);
      window.CoinDenLanguage.run(window, isHome);
      visit.tap = (lang) =>
        visit.click({ target: { closest: () => ({ getAttribute: () => lang }) } });
      return visit;
    },
  };
}

const cases = [];
function test(name, fn) {
  cases.push([name, fn]);
}

test("John con el navegador en inglés llega a la versión en inglés", () => {
  assert.equal(browser("en-US").open(true).redirectedTo, "/coinden/en/");
});

test("Ana con el navegador en portugués llega a la versión en portugués", () => {
  assert.equal(browser("pt-BR").open(true).redirectedTo, "/coinden/pt/");
});

test("Carlos con el navegador en español se queda en español", () => {
  assert.equal(browser("es-BO").open(true).redirectedTo, null);
});

test("Wei con el navegador en chino se queda en español", () => {
  assert.equal(browser("zh-CN").open(true).redirectedTo, null);
});

test("solo la portada redirige", () => {
  assert.equal(browser("en-US").open(false).redirectedTo, null);
});

test("una sola vez por visita: volver a la portada no rebota", () => {
  const john = browser("en-US");
  assert.equal(john.open(true).redirectedTo, "/coinden/en/");
  assert.equal(john.open(true).redirectedTo, null);
});

test("John toca ES en la versión en inglés y la portada no lo vuelve a llevar al inglés", () => {
  const john = browser("en-US");
  const home = john.open(true);
  assert.equal(home.redirectedTo, "/coinden/en/");
  john.open(false).tap("es");
  assert.equal(john.open(true).redirectedTo, null);
});

test("una elección manual se respeta aunque sea una visita nueva", () => {
  const ana = browser("pt-BR");
  ana.open(false).tap("es");
  assert.equal(ana.open(true).redirectedTo, null);
});

test("con el almacenamiento bloqueado no se redirige", () => {
  assert.equal(browser("en-US", { blocked: true }).open(true).redirectedTo, null);
});

test("Carlos abre la portada y no se hace ninguna consulta externa", () => {
  for (const lang of ["es-BO", "en-US", "pt-BR", "zh-CN"]) {
    const visitor = browser(lang);
    visitor.open(true);
    assert.deepEqual(visitor.requests, []);
  }
  assert.doesNotMatch(code, /fetch|XMLHttpRequest|geoloc|ipapi|ipinfo|country/i);
});

let failed = 0;
for (const [name, fn] of cases) {
  try {
    fn();
    console.log(`ok   ${name}`);
  } catch (error) {
    failed++;
    console.log(`FAIL ${name}\n     ${error.message}`);
  }
}
console.log(`${cases.length - failed}/${cases.length} escenarios`);
process.exit(failed ? 1 : 0);
