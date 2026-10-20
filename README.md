# AutoMine (Fabric 1.21.11, solo client)

Scava automaticamente i blocchi che hai scelto: basta guardarli, senza cliccare.

## Comandi (in chat)

| Comando | Cosa fa |
|---|---|
| `/automine` | Apre il layout per selezionare i blocchi (click = seleziona/deseleziona) |
| `/automine on` | Attiva lo scavo automatico |
| `/automine off` | Lo disattiva |
| `/automine toggle` | Inverte lo stato |
| `/automine status` | Mostra stato e numero di blocchi selezionati |

Tasti rapidi (modificabili in Opzioni > Controlli): **R** attiva/disattiva, **G** apre il menu.

## Nel menu
- Barra di ricerca (per nome o id), rotellina per scorrere.
- **Pulisci**: deseleziona tutto. **Solo minerali**: seleziona tutti i minerali.
- **Protezione attrezzo**: si ferma quando all'attrezzo restano 5 usi.
- La selezione viene salvata in `config/automine.json`.

## Build

Servono Java 21 e Gradle (o il wrapper del template Fabric).

```
gradle wrapper --gradle-version 9.2.1   # solo la prima volta, genera ./gradlew
./gradlew build
```

Il jar si trova in `build/libs/automine-1.0.0.jar`: mettilo nella cartella `mods` insieme a Fabric API.
