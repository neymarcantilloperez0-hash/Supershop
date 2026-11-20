# SuperShop — versión mejorada

Plugin de tienda GUI para Spigot/Paper 1.20.4, Java 17+, Vault y un proveedor de economía compatible.

## Funciones

- Menú principal de 54 espacios con categorías y saldo del jugador.
- 11 categorías configurables: Madera, Bloques, Minerales, Comida, Equipamiento, Drops de Mobs, Decoración, Spawners, Especial, Redstone y Utilidad.
- Catálogo ampliado de más de 250 entradas, con las familias de madera separadas por tipo.
- Menú de compra independiente con selector `-64 / -10 / -1 / +1 / +10 / +64`, reinicio, resumen de precio, cancelar y confirmar.
- Compra con comprobación de saldo y espacio de inventario; no tira al suelo los artículos que no caben.
- Venta por unidad, venta de todo el tipo y venta de toda la categoría.
- Los iconos de catálogo llevan identificadores internos, evitando buscar artículos por el nombre visible.
- Los spawners llevan la entidad configurada y se identifican separadamente al venderlos.
- Animación breve del marco al abrir menús, sonidos y partículas configurables.
- PlaceholderAPI: `%shop_balance%` y `%shop_balance_raw%`.
- Recarga con `/shop reload`.

## Instalación

1. Compila el proyecto con Maven: `mvn clean package`.
2. Copia `target/SuperShop.jar` a la carpeta `plugins` del servidor.
3. Asegúrate de tener Vault y un plugin de economía compatible instalados.
4. Inicia el servidor y edita `plugins/SuperShop/config.yml` y `plugins/SuperShop/shop.yml`.
5. Ejecuta `/shop reload` después de editar la configuración.

## Spawners

Por seguridad económica, están desactivados por defecto. En `config.yml` cambia:

```yaml
shop:
  spawners:
    enabled: true
```

Luego ejecuta `/shop reload`.

## Configuración de precios

En `shop.yml`, cada artículo usa `material`, `name`, `buy` y `sell`. Usa `-1` para deshabilitar compra o venta. Los precios son unidades de la moneda configurada en Vault.

## Compatibilidad

El `pom.xml` apunta a Spigot API 1.20.4 y compila para Java 17. Para servidores con otra versión de Minecraft puede ser necesario ajustar la dependencia y validar materiales disponibles en esa versión.
