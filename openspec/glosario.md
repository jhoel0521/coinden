# Glosario: lo que dice el spec y lo que ve la persona

CoinDen es una libreta de gastos. Los specs, el dominio y el código usan palabras de contabilidad (billetera, saldo, tasa) porque describen cómo se calcula cada cosa. La persona, el sitio y la ficha de Play ven otro vocabulario, para que nada se confunda con un servicio financiero (pagos, cuentas con fondos, casas de cambio).

**Regla:** cuando un spec o el código dice un término de la columna izquierda, en pantalla, en el sitio y en la ficha se escribe el de la derecha. Los nombres internos (clases, enums, claves de recursos, paquetes) no se renombran. Los textos nuevos de la interfaz no deben usar la columna izquierda: una prueba (`VisibleVocabularyTest`) lo comprueba en español, inglés y portugués.

| En el spec y en el código | Lo que se ve (es) | en | pt |
|---|---|---|---|
| billetera (`Wallet`) | fuente | source | fonte |
| saldo, saldo de hoy, ajuste de saldo | total, total de hoy, ajuste | total, today's total, adjustment | total, total de hoje, ajuste |
| patrimonio total o parcial | total anotado, total anotado parcial | recorded total, partial recorded total | total anotado, total anotado parcial |
| tasa, cotización (`ExchangeRate`) | equivalencia | equivalence | equivalência |
| pago | gasto | expense | gasto |
| tipo BANK | Cuenta | Account | Conta |
| tipo CRYPTO | Personalizada | Custom | Personalizada |
| tipo CREDIT | Tarjeta | Card | Cartão |
| movimiento TRANSFER | Traspaso | Move | Repasse |

## Lo que no se traduce a otro vocabulario

- El medio de pago "Transferencia o QR" (`method_transfer`): describe cómo pagó la persona, como lo dice su comprobante.
- Los nombres que escribe la persona (por ejemplo "BNB Bs") y los alias que dice al dictar ("bnb", "meru"): son sus datos.
- Los códigos de moneda (BOB, USD, ARS…), "comprobante", "monto", "gasto" e "ingreso".

## Cómo leer un spec antiguo

Los specs archivados y los de `openspec/specs/` anteriores a la 0.2.1 dicen "billetera", "saldo" y "tasa" en sus escenarios. Se leen con este glosario: el comportamiento es el mismo y solo cambia el texto visible. Un spec nuevo que describa una pantalla debe usar la columna derecha en lo que se ve.
