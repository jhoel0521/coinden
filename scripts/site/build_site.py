"""Genera el sitio estático de CoinDen en site/ (GitHub Pages).

Uso (desde la raíz del repo):  python scripts/site/build_site.py

Una sola fuente para las tres lenguas: inicio, política de privacidad y apoyar. Sin JavaScript de
terceros, sin analítica y sin cookies. El logo sale de los recursos de la app.
"""

import html
import os
import shutil

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", ".."))
SITE = os.path.join(ROOT, "site")
BASE = "/coinden"
REPO = "https://github.com/jhoel0521/coinden"
SPONSORS = "https://github.com/sponsors/jhoel0521"
# PayPal.me se agrega cuando la persona confirme que puede recibir y retirar en Bolivia.
PAYPAL = None
UPDATED = {"es": "19 de octubre de 2026", "en": "October 19, 2026", "pt": "19 de outubro de 2026"}

# Ruta de cada página por idioma (el español va en la raíz).
PATHS = {
    "home": {"es": "/", "en": "/en/", "pt": "/pt/"},
    "privacy": {"es": "/privacidad/", "en": "/en/privacy/", "pt": "/pt/privacidade/"},
    "support": {"es": "/apoyar/", "en": "/en/support/", "pt": "/pt/apoiar/"},
}

T = {
    "es": {
        "lang": "es",
        "tagline": "Tu guarida de monedas",
        "nav": ("Inicio", "Privacidad", "Apoyar", "Código"),
        "home_title": "CoinDen: finanzas personales sin servidores",
        "home_intro": "Anota en segundos lo que gastas, en la moneda que sea, y revísalo con calma en la noche. "
        "Sin cuenta, sin servidores, sin publicidad: tus datos viven cifrados en tu teléfono.",
        "home_points": [
            "Dicta, saca una foto o comparte un comprobante: CoinDen arma un borrador y tú lo confirmas.",
            "Varias monedas y la tasa que de verdad usas, no la oficial.",
            "Todo se queda en tu teléfono, cifrado. Puedes protegerlo con tu huella o PIN.",
            "Código abierto bajo la licencia MPL-2.0.",
        ],
        "home_play": "Muy pronto en Google Play.",
        "privacy_title": "Política de privacidad",
        "privacy": [
            ("Resumen", [
                "CoinDen no recolecta tus datos. No hay cuenta, ni servidores de CoinDen, ni publicidad, ni analítica propia. "
                "Lo que registras se guarda cifrado en tu teléfono y no se envía a ningún lado.",
            ]),
            ("Qué datos maneja la app y dónde viven", [
                "Tus billeteras, movimientos, borradores, categorías, tasas y notas, y las fotos, PDF y audios de tus comprobantes, "
                "se guardan solo en tu teléfono, en una base cifrada (AES-256) cuya llave protege el almacén seguro de Android.",
                "La app no hace copias de seguridad automáticas en la nube. Si borras los datos de la app o la desinstalas, se borran.",
            ]),
            ("Permisos", [
                "Cámara: solo cuando tocas Foto, para fotografiar un comprobante.",
                "Micrófono: solo mientras dictas. La voz se convierte en texto dentro del teléfono.",
                "Internet: solo para descargar los modelos de voz cuando tú lo pides.",
                "Bloqueo con huella o PIN: lo verifica el sistema de tu teléfono. CoinDen nunca recibe ni guarda tu huella ni tu PIN.",
            ]),
            ("Qué puede salir del teléfono", [
                "Modelos de voz: si los descargas, la app los pide a Hugging Face (huggingface.co). Es una descarga pública: "
                "no se envía nada tuyo, aunque, como en cualquier visita a un sitio web, ese servidor ve tu dirección IP.",
                "Lector mejorado de Google (opcional, apagado por defecto): si lo activas, las fotos se leen también con ML Kit "
                "de Google dentro del teléfono, y Google recibe solo datos técnicos de uso (modelo y sistema del teléfono, versión "
                "de la app, un identificador de esta instalación, tiempos, tamaños, eventos y errores). Nunca recibe tus fotos, "
                "textos ni montos. Lo puedes apagar cuando quieras en Perfil → Privacidad. Más información en las "
                "<a href=\"https://developers.google.com/ml-kit/terms\">condiciones de ML Kit</a>.",
                "Lo que compartes con CoinDen desde otras apps se guarda en tu teléfono, como todo lo demás.",
            ]),
            ("Lo que nunca hacemos", [
                "No vendemos, alquilamos ni compartimos datos. No mostramos publicidad. No hay rastreadores.",
            ]),
            ("Cambios futuros", [
                "Si en el futuro CoinDen suma funciones que usen la red (cotizaciones públicas o un respaldo cifrado en tu propia nube), "
                "serán opcionales, se explicarán en la app antes de activarse y esta política se actualizará primero.",
            ]),
            ("Menores", [
                "CoinDen no está dirigida a menores de 13 años.",
            ]),
            ("Contacto", [
                f"Para dudas o pedidos sobre privacidad, abre un issue en <a href=\"{REPO}/issues\">el repositorio de CoinDen</a>.",
            ]),
        ],
        "updated": "Vigente desde el",
        "support_title": "Apoya CoinDen",
        "support_intro": "CoinDen es gratis para siempre, sin publicidad y sin venta de datos. Vive de donaciones.",
        "support_sponsors": "Con GitHub Sponsors (necesitas una cuenta de GitHub, es gratis):",
        "support_sponsors_btn": "Donar con GitHub Sponsors",
        "support_paypal": "Con PayPal:",
        "support_other": "También ayudas si compartes CoinDen, reportas fallos o propones traducciones en el repositorio.",
    },
    "en": {
        "lang": "en",
        "tagline": "Your coin den",
        "nav": ("Home", "Privacy", "Support", "Code"),
        "home_title": "CoinDen: personal finance without servers",
        "home_intro": "Record what you spend in seconds, in any currency, and review it calmly at night. "
        "No account, no servers, no ads: your data lives encrypted on your phone.",
        "home_points": [
            "Dictate, take a photo or share a receipt: CoinDen drafts it and you confirm it.",
            "Several currencies and the rate you actually use, not the official one.",
            "Everything stays on your phone, encrypted. You can protect it with your fingerprint or PIN.",
            "Open source under the MPL-2.0 license.",
        ],
        "home_play": "Coming soon to Google Play.",
        "privacy_title": "Privacy policy",
        "privacy": [
            ("Summary", [
                "CoinDen does not collect your data. There is no account, no CoinDen servers, no ads and no analytics of its own. "
                "What you record is stored encrypted on your phone and is not sent anywhere.",
            ]),
            ("What data the app handles and where it lives", [
                "Your wallets, movements, drafts, categories, rates and notes, and the photos, PDFs and audio of your receipts, "
                "are stored only on your phone, in an encrypted database (AES-256) whose key is protected by Android's secure storage.",
                "The app makes no automatic cloud backups. If you clear the app's data or uninstall it, everything is deleted.",
            ]),
            ("Permissions", [
                "Camera: only when you tap Photo, to photograph a receipt.",
                "Microphone: only while you dictate. Speech is turned into text on the phone.",
                "Internet: only to download voice models when you ask for them.",
                "Fingerprint or PIN lock: checked by your phone's system. CoinDen never receives or stores your fingerprint or PIN.",
            ]),
            ("What can leave the phone", [
                "Voice models: if you download them, the app requests them from Hugging Face (huggingface.co). It is a public download: "
                "nothing of yours is sent, although, as with any website visit, that server sees your IP address.",
                "Enhanced Google reader (optional, off by default): if you turn it on, photos are also read with Google's ML Kit "
                "on the phone, and Google receives only technical usage data (phone model and system, app version, an identifier "
                "for this installation, timings, sizes, events and errors). It never receives your photos, texts or amounts. "
                "You can turn it off at any time in Profile → Privacy. More in the "
                "<a href=\"https://developers.google.com/ml-kit/terms\">ML Kit terms</a>.",
                "What you share with CoinDen from other apps is stored on your phone, like everything else.",
            ]),
            ("What we never do", [
                "We do not sell, rent or share data. We show no ads. There are no trackers.",
            ]),
            ("Future changes", [
                "If CoinDen adds features that use the network (public exchange rates or an encrypted backup to your own cloud), "
                "they will be optional, explained in the app before they are turned on, and this policy will be updated first.",
            ]),
            ("Children", [
                "CoinDen is not directed at children under 13.",
            ]),
            ("Contact", [
                f"For privacy questions or requests, open an issue in <a href=\"{REPO}/issues\">the CoinDen repository</a>.",
            ]),
        ],
        "updated": "Effective",
        "support_title": "Support CoinDen",
        "support_intro": "CoinDen is free forever, with no ads and no data sales. It lives on donations.",
        "support_sponsors": "With GitHub Sponsors (you need a free GitHub account):",
        "support_sponsors_btn": "Donate with GitHub Sponsors",
        "support_paypal": "With PayPal:",
        "support_other": "You also help by sharing CoinDen, reporting bugs or suggesting translations in the repository.",
    },
    "pt": {
        "lang": "pt",
        "tagline": "Seu covil de moedas",
        "nav": ("Início", "Privacidade", "Apoiar", "Código"),
        "home_title": "CoinDen: finanças pessoais sem servidores",
        "home_intro": "Anote em segundos o que você gasta, em qualquer moeda, e revise com calma à noite. "
        "Sem conta, sem servidores, sem anúncios: seus dados ficam criptografados no seu telefone.",
        "home_points": [
            "Dite, tire uma foto ou compartilhe um comprovante: o CoinDen monta um rascunho e você confirma.",
            "Várias moedas e a taxa que você realmente usa, não a oficial.",
            "Tudo fica no seu telefone, criptografado. Você pode protegê-lo com sua digital ou PIN.",
            "Código aberto sob a licença MPL-2.0.",
        ],
        "home_play": "Em breve no Google Play.",
        "privacy_title": "Política de privacidade",
        "privacy": [
            ("Resumo", [
                "O CoinDen não coleta seus dados. Não há conta, nem servidores do CoinDen, nem anúncios, nem análise própria. "
                "O que você registra fica criptografado no seu telefone e não é enviado a lugar nenhum.",
            ]),
            ("Quais dados o app usa e onde ficam", [
                "Suas carteiras, movimentos, rascunhos, categorias, taxas e notas, e as fotos, PDFs e áudios dos seus comprovantes, "
                "ficam só no seu telefone, em um banco criptografado (AES-256) cuja chave é protegida pelo armazenamento seguro do Android.",
                "O app não faz cópias de segurança automáticas na nuvem. Se você apagar os dados do app ou desinstalá-lo, tudo é apagado.",
            ]),
            ("Permissões", [
                "Câmera: só quando você toca em Foto, para fotografar um comprovante.",
                "Microfone: só enquanto você dita. A voz vira texto dentro do telefone.",
                "Internet: só para baixar os modelos de voz quando você pede.",
                "Bloqueio com digital ou PIN: é verificado pelo sistema do telefone. O CoinDen nunca recebe nem guarda sua digital ou PIN.",
            ]),
            ("O que pode sair do telefone", [
                "Modelos de voz: se você os baixar, o app os pede ao Hugging Face (huggingface.co). É um download público: "
                "nada seu é enviado, embora, como em qualquer visita a um site, esse servidor veja seu endereço IP.",
                "Leitor aprimorado do Google (opcional, desligado por padrão): se você ativar, as fotos também são lidas com o ML Kit "
                "do Google dentro do telefone, e o Google recebe só dados técnicos de uso (modelo e sistema do telefone, versão do app, "
                "um identificador desta instalação, tempos, tamanhos, eventos e erros). Nunca recebe suas fotos, textos ou valores. "
                "Você pode desligar quando quiser em Perfil → Privacidade. Mais nos "
                "<a href=\"https://developers.google.com/ml-kit/terms\">termos do ML Kit</a>.",
                "O que você compartilha com o CoinDen de outros apps fica no seu telefone, como todo o resto.",
            ]),
            ("O que nunca fazemos", [
                "Não vendemos, alugamos nem compartilhamos dados. Não mostramos anúncios. Não há rastreadores.",
            ]),
            ("Mudanças futuras", [
                "Se o CoinDen ganhar funções que usem a rede (cotações públicas ou um backup criptografado na sua própria nuvem), "
                "elas serão opcionais, explicadas no app antes de serem ativadas, e esta política será atualizada antes.",
            ]),
            ("Crianças", [
                "O CoinDen não é dirigido a menores de 13 anos.",
            ]),
            ("Contato", [
                f"Para dúvidas ou pedidos sobre privacidade, abra uma issue no <a href=\"{REPO}/issues\">repositório do CoinDen</a>.",
            ]),
        ],
        "updated": "Vigente desde",
        "support_title": "Apoie o CoinDen",
        "support_intro": "O CoinDen é grátis para sempre, sem anúncios e sem venda de dados. Vive de doações.",
        "support_sponsors": "Com GitHub Sponsors (você precisa de uma conta gratuita no GitHub):",
        "support_sponsors_btn": "Doar com GitHub Sponsors",
        "support_paypal": "Com PayPal:",
        "support_other": "Você também ajuda compartilhando o CoinDen, relatando falhas ou sugerindo traduções no repositório.",
    },
}

CSS = """:root{--bg:#FFF7F2;--fg:#231A15;--muted:#6B5B52;--primary:#9A4A25;--card:#FFFFFF;--line:#E9D9CF}
@media (prefers-color-scheme:dark){:root{--bg:#1A1411;--fg:#F3E7DF;--muted:#C2B0A5;--primary:#F2B08A;--card:#251C17;--line:#3A2C24}}
*{box-sizing:border-box}body{margin:0;background:var(--bg);color:var(--fg);font:17px/1.6 system-ui,-apple-system,"Segoe UI",Roboto,sans-serif}
main,header,footer{max-width:760px;margin:0 auto;padding:0 20px}
header{display:flex;flex-wrap:wrap;align-items:center;gap:12px;padding-top:24px}
header img{width:56px;height:56px}.brand{font-weight:800;font-size:1.5rem}.brand b{color:var(--primary)}
nav{display:flex;flex-wrap:wrap;gap:6px 16px;width:100%}nav a,a{color:var(--primary)}
.langs{margin-left:auto;font-size:.9rem}.langs a{margin-left:8px}
h1{font-size:1.8rem;line-height:1.25}h2{font-size:1.2rem;margin-top:1.8em}
.card{background:var(--card);border:1px solid var(--line);border-radius:16px;padding:16px 20px;margin:16px 0}
.btn{display:inline-block;background:var(--primary);color:var(--bg);padding:12px 20px;border-radius:999px;text-decoration:none;font-weight:700}
.muted{color:var(--muted)}footer{padding:32px 20px;font-size:.9rem}
"""


def href(page: str, lang: str) -> str:
    return BASE + PATHS[page][lang]


def page(lang: str, current: str, title: str, body: str) -> str:
    t = T[lang]
    labels = t["nav"]
    links = [("home", labels[0]), ("privacy", labels[1]), ("support", labels[2])]
    nav = "".join(
        f'<a href="{href(p, lang)}"{" aria-current=\"page\"" if p == current else ""}>{html.escape(name)}</a>'
        for p, name in links
    ) + f'<a href="{REPO}">{html.escape(labels[3])}</a>'
    langs = "".join(f'<a href="{href(current, other)}" hreflang="{other}">{other.upper()}</a>' for other in ("es", "en", "pt") if other != lang)
    alternates = "".join(f'<link rel="alternate" hreflang="{o}" href="https://jhoel0521.github.io{href(current, o)}">' for o in ("es", "en", "pt"))
    return f"""<!doctype html>
<html lang="{lang}">
<head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<title>{html.escape(title)}</title>
<meta name="description" content="{html.escape(t['home_intro'])}">
{alternates}
<link rel="icon" href="{BASE}/img/coinden-icon.png">
<link rel="stylesheet" href="{BASE}/estilo.css">
</head>
<body>
<header>
<img src="{BASE}/img/coinden-logo.webp" alt="">
<div><div class="brand">Coin<b>Den</b></div><div class="muted">{html.escape(t['tagline'])}</div></div>
<div class="langs">{langs}</div>
<nav>{nav}</nav>
</header>
<main>
{body}
</main>
<footer class="muted">CoinDen · MPL-2.0 · <a href="{REPO}">GitHub</a></footer>
</body>
</html>
"""


def home(lang: str) -> str:
    t = T[lang]
    points = "".join(f"<li>{html.escape(p)}</li>" for p in t["home_points"])
    body = f"""<h1>{html.escape(t['home_title'])}</h1>
<p>{html.escape(t['home_intro'])}</p>
<ul>{points}</ul>
<p class="card">{html.escape(t['home_play'])}</p>
<p><a class="btn" href="{href('support', lang)}">{html.escape(t['support_title'])}</a></p>"""
    return page(lang, "home", t["home_title"], body)


def privacy(lang: str) -> str:
    t = T[lang]
    sections = "".join(
        f"<h2>{html.escape(title)}</h2>" + "".join(f"<p>{text}</p>" for text in paragraphs) for title, paragraphs in t["privacy"]
    )
    body = f"""<h1>{html.escape(t['privacy_title'])}</h1>
<p class="muted">{html.escape(t['updated'])} {html.escape(UPDATED[lang])}</p>
{sections}"""
    return page(lang, "privacy", f"{t['privacy_title']} · CoinDen", body)


def support(lang: str) -> str:
    t = T[lang]
    paypal = f'<div class="card"><p>{html.escape(t["support_paypal"])}</p><p><a class="btn" href="{PAYPAL}">PayPal</a></p></div>' if PAYPAL else ""
    body = f"""<h1>{html.escape(t['support_title'])}</h1>
<p>{html.escape(t['support_intro'])}</p>
<div class="card"><p>{html.escape(t['support_sponsors'])}</p><p><a class="btn" href="{SPONSORS}">{html.escape(t['support_sponsors_btn'])}</a></p></div>
{paypal}
<p class="muted">{html.escape(t['support_other'])}</p>"""
    return page(lang, "support", f"{t['support_title']} · CoinDen", body)


def write(path: str, content: str) -> None:
    full = os.path.join(SITE, path.strip("/"), "index.html")
    os.makedirs(os.path.dirname(full), exist_ok=True)
    with open(full, "w", encoding="utf-8", newline="\n") as f:
        f.write(content)
    print("  ", os.path.relpath(full, ROOT))


def main() -> None:
    if os.path.isdir(SITE):
        shutil.rmtree(SITE)
    os.makedirs(os.path.join(SITE, "img"))
    with open(os.path.join(SITE, "estilo.css"), "w", encoding="utf-8", newline="\n") as f:
        f.write(CSS)
    # Sin Jekyll: GitHub Pages sirve los archivos tal cual.
    open(os.path.join(SITE, ".nojekyll"), "w").close()
    res = os.path.join(ROOT, "composeApp", "src", "commonMain", "composeResources", "drawable-xxhdpi")
    shutil.copy(os.path.join(res, "coinden_logo.webp"), os.path.join(SITE, "img", "coinden-logo.webp"))
    shutil.copy(os.path.join(ROOT, "design", "brand", "coinden-icono-app-1024.png"), os.path.join(SITE, "img", "coinden-icon.png"))
    for lang in ("es", "en", "pt"):
        write(PATHS["home"][lang], home(lang))
        write(PATHS["privacy"][lang], privacy(lang))
        write(PATHS["support"][lang], support(lang))


if __name__ == "__main__":
    main()
