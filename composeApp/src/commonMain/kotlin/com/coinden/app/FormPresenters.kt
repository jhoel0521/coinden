package com.coinden.app

import com.coinden.app.capture.DraftFormPresenter

/**
 * Los formularios abiertos, por entrada de navegación. Vive con el proceso, no con la pantalla:
 * cuando Android recrea la pantalla (rotar, cambiar el tamaño de letra o el idioma del sistema)
 * la entrada conserva su id y el formulario sigue con lo que la persona escribió.
 */
class FormPresenters {
    private val forms = mutableMapOf<String, DraftFormPresenter>()

    fun getOrPut(
        entryId: String,
        create: () -> DraftFormPresenter,
    ): DraftFormPresenter = forms.getOrPut(entryId, create)

    /** Al guardar, descartar o salir, el formulario se suelta. */
    fun remove(entryId: String) {
        forms.remove(entryId)
    }
}
