// Idioma del sitio. La portada (español) lleva una sola vez a /en/ o /pt/ según el idioma que
// declara el navegador (navigator.language). Sin IP, sin país y sin servicios externos: nada sale
// de la página. Tocar ES, EN o PT guarda la elección y desde entonces la portada no redirige.
// Si el navegador bloquea el almacenamiento, no se redirige y la portada queda en español.
(function (global) {
  var CHOICE = "coinden-lang";
  var REDIRECTED = "coinden-lang-redirected";
  var PATHS = { en: "/coinden/en/", pt: "/coinden/pt/" };

  /** A dónde llevar desde la portada, o null para quedarse en español. */
  function target(language, choice, redirected) {
    if (choice) return null;
    if (redirected) return null;
    var primary = String(language || "").toLowerCase().split("-")[0];
    return PATHS[primary] ? primary : null;
  }

  /** Enlaza los botones de idioma y, en la portada, redirige si corresponde. */
  function run(win, isHome) {
    try {
      var local = win.localStorage;
      var session = win.sessionStorage;
      win.document.addEventListener("click", function (event) {
        var link = event.target && event.target.closest ? event.target.closest(".langs a[hreflang]") : null;
        if (!link) return;
        try {
          local.setItem(CHOICE, link.getAttribute("hreflang"));
        } catch (e) {}
      });
      if (!isHome) return;
      var lang = target(win.navigator.language, local.getItem(CHOICE), session.getItem(REDIRECTED));
      if (!lang) return;
      session.setItem(REDIRECTED, "1");
      // replace: "Atrás" no vuelve a la portada para rebotar otra vez.
      win.location.replace(PATHS[lang]);
    } catch (e) {
      // Sin almacenamiento no se puede recordar la elección: mejor no redirigir.
    }
  }

  global.CoinDenLanguage = { target: target, run: run };
  if (typeof document !== "undefined" && document.currentScript) {
    run(window, document.currentScript.hasAttribute("data-portada"));
  }
})(typeof window !== "undefined" ? window : globalThis);
