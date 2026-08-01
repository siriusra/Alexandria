package com.alexandria.app.ui.components

import androidx.annotation.DrawableRes
import com.alexandria.app.R

data class GameIcon(
    val key: String,
    val label: String,
    @DrawableRes val resId: Int,
    val category: String,
    val tags: String = ""
)

object CharacterIcons {

    val categoryNames: Map<String, String> = mapOf(
        "personas" to "Personas",
        "heroes" to "Héroes",
        "criaturas" to "Criaturas",
        "detectives" to "Detectives",
        "historia" to "Historia",
        "drama" to "Drama",
        "misterio" to "Misterio",
        "sci_fi" to "Ciencia ficción",
        "objetos" to "Objetos",
        "naturaleza" to "Naturaleza"
    )

    val categoryKeys: List<String> = listOf(
        "personas", "heroes", "criaturas", "detectives", "historia",
        "drama", "misterio", "sci_fi", "objetos", "naturaleza"
    )

    val icons: List<GameIcon> = listOf(
        GameIcon("male", "Hombre", R.drawable.ic_char_male, "personas", "varon,chico,señor"),
        GameIcon("female", "Mujer", R.drawable.ic_char_female, "personas", "chica,señora,dama"),
        GameIcon("swordman", "Guerrero", R.drawable.ic_char_swordman, "personas", "soldado,combatiente,hombre"),
        GameIcon("swordwoman", "Guerrera", R.drawable.ic_char_swordwoman, "personas", "soldado,combatiente,mujer"),
        GameIcon("three_friends", "Amigos", R.drawable.ic_char_three_friends, "personas", "compañeros,grupo,amistad"),
        GameIcon("family", "Familia", R.drawable.ic_char_family, "personas", "padres,padre,madre,hijos"),
        GameIcon("sword", "Espada", R.drawable.ic_char_sword, "heroes"),
        GameIcon("swords", "Espadas", R.drawable.ic_char_swords, "heroes"),
        GameIcon("shield", "Escudo", R.drawable.ic_char_shield, "heroes"),
        GameIcon("wand", "Varita", R.drawable.ic_char_wand, "heroes"),
        GameIcon("staff", "Báculo", R.drawable.ic_char_staff, "heroes"),
        GameIcon("knight", "Caballero", R.drawable.ic_char_knight, "heroes"),
        GameIcon("elf", "Elfo", R.drawable.ic_char_elf, "heroes"),
        GameIcon("dwarf", "Enano", R.drawable.ic_char_dwarf, "heroes"),
        GameIcon("mage", "Mago", R.drawable.ic_char_mage, "heroes", "hechicero,brujo"),
        GameIcon("king", "Rey", R.drawable.ic_char_king, "heroes"),
        GameIcon("queen", "Reina", R.drawable.ic_char_queen, "heroes"),
        GameIcon("crown", "Corona", R.drawable.ic_char_crown, "heroes"),
        GameIcon("fairy", "Hada", R.drawable.ic_char_fairy, "heroes"),
        GameIcon("archer", "Arquero", R.drawable.ic_char_archer, "heroes"),
        GameIcon("viking", "Vikingo", R.drawable.ic_char_viking, "heroes"),
        GameIcon("pirate", "Pirata", R.drawable.ic_char_pirate, "heroes"),
        GameIcon("wolf", "Lobo", R.drawable.ic_char_wolf, "criaturas"),
        GameIcon("dragon", "Dragón", R.drawable.ic_char_dragon, "criaturas"),
        GameIcon("goblin", "Goblin", R.drawable.ic_char_goblin, "criaturas"),
        GameIcon("ogre", "Ogro", R.drawable.ic_char_ogre, "criaturas"),
        GameIcon("ghost", "Fantasma", R.drawable.ic_char_ghost, "criaturas"),
        GameIcon("vampire", "Vampiro", R.drawable.ic_char_vampire, "criaturas"),
        GameIcon("zombie", "Zombi", R.drawable.ic_char_zombie, "criaturas"),
        GameIcon("skull", "Calavera", R.drawable.ic_char_skull, "criaturas"),
        GameIcon("raven", "Cuervo", R.drawable.ic_char_raven, "criaturas"),
        GameIcon("fox", "Zorro", R.drawable.ic_char_fox, "criaturas"),
        GameIcon("moon_bats", "Murciélago", R.drawable.ic_char_moon_bats, "criaturas", "murcielagos,bat"),
        GameIcon("flashlight", "Linterna", R.drawable.ic_char_flashlight, "detectives", "detective,linterna"),
        GameIcon("telescope", "Telescopio", R.drawable.ic_char_telescope, "detectives"),
        GameIcon("question", "Interrogante", R.drawable.ic_char_question, "detectives", "duda,misterio,pregunta"),
        GameIcon("crime_tape", "Cinta policial", R.drawable.ic_char_crime_tape, "detectives", "policia,crimen,detective"),
        GameIcon("archive_research", "Archivo", R.drawable.ic_char_archive_research, "detectives", "investigacion,expediente,detective"),
        GameIcon("lock_spy", "Candado espía", R.drawable.ic_char_lock_spy, "detectives", "espiar,cerradura,detective"),
        GameIcon("think", "Pensar", R.drawable.ic_char_think, "detectives", "pensamiento,detective"),
        GameIcon("footprint", "Huella", R.drawable.ic_char_footprint, "detectives", "pista,detective,huellas"),
        GameIcon("roman_toga", "Toga romana", R.drawable.ic_char_roman_toga, "historia", "roma,romano,antigua"),
        GameIcon("spartan_helmet", "Casco espartano", R.drawable.ic_char_spartan_helmet, "historia", "esparta,griego,antigua"),
        GameIcon("ancient_columns", "Columnas antiguas", R.drawable.ic_char_ancient_columns, "historia", "griego,romano,templo"),
        GameIcon("ancient_ruins", "Ruinas antiguas", R.drawable.ic_char_ancient_ruins, "historia", "antigua,ruinas,templo"),
        GameIcon("classical_knowledge", "Sabiduría clásica", R.drawable.ic_char_classical_knowledge, "historia", "filosofia,antigua,sabio"),
        GameIcon("hourglass", "Reloj de arena", R.drawable.ic_char_hourglass, "historia", "tiempo,antiguo,ampolleta"),
        GameIcon("empty_hourglass", "Reloj vacío", R.drawable.ic_char_empty_hourglass, "historia", "tiempo,antiguo,ampolleta"),
        GameIcon("caveman", "Hombre prehistórico", R.drawable.ic_char_caveman, "historia", "prehistoria,cavernicola"),
        GameIcon("drama_masks", "Máscaras de teatro", R.drawable.ic_char_drama_masks, "drama", "teatro,comedia,tragedia"),
        GameIcon("theater", "Teatro", R.drawable.ic_char_theater, "drama"),
        GameIcon("theater_curtains", "Telón de teatro", R.drawable.ic_char_theater_curtains, "drama", "telon,escenario"),
        GameIcon("film_strip", "Película", R.drawable.ic_char_film_strip, "drama", "cine,film,peliculas"),
        GameIcon("film_projector", "Proyector", R.drawable.ic_char_film_projector, "drama", "cine,film"),
        GameIcon("candles", "Velas", R.drawable.ic_char_candles, "misterio", "vela,oscuridad"),
        GameIcon("old_lantern", "Farol antiguo", R.drawable.ic_char_old_lantern, "misterio", "luz,antiguo,linterna"),
        GameIcon("barn_owl", "Lechuza", R.drawable.ic_char_barn_owl, "misterio", "buho,ave"),
        GameIcon("locked_box", "Caja cerrada", R.drawable.ic_char_locked_box, "misterio", "cerrada,secreto,cofre"),
        GameIcon("boss_key", "Llave maestra", R.drawable.ic_char_boss_key, "misterio", "llave,secreto"),
        GameIcon("key_lock", "Candado y llave", R.drawable.ic_char_key_lock, "misterio", "llave,cerradura"),
        GameIcon("egyptian_sphinx", "Esfinge egipcia", R.drawable.ic_char_egyptian_sphinx, "misterio", "egipto,enigma"),
        GameIcon("greek_sphinx", "Esfinge griega", R.drawable.ic_char_greek_sphinx, "misterio", "griega,enigma"),
        GameIcon("spell_book", "Libro de hechizos", R.drawable.ic_char_spell_book, "misterio", "hechizos,encantamiento"),
        GameIcon("robot", "Robot", R.drawable.ic_char_robot, "sci_fi"),
        GameIcon("alien", "Alienígena", R.drawable.ic_char_alien, "sci_fi"),
        GameIcon("astronaut", "Astronauta", R.drawable.ic_char_astronaut, "sci_fi"),
        GameIcon("rocket", "Cohete", R.drawable.ic_char_rocket, "sci_fi"),
        GameIcon("planet", "Planeta", R.drawable.ic_char_planet, "sci_fi"),
        GameIcon("key", "Llave", R.drawable.ic_char_key, "objetos"),
        GameIcon("chest", "Cofre", R.drawable.ic_char_chest, "objetos"),
        GameIcon("map", "Mapa", R.drawable.ic_char_map, "objetos"),
        GameIcon("scroll", "Pergamino", R.drawable.ic_char_scroll, "objetos"),
        GameIcon("quill", "Pluma", R.drawable.ic_char_quill, "objetos"),
        GameIcon("book", "Libro", R.drawable.ic_char_book, "objetos"),
        GameIcon("lamp", "Lámpara", R.drawable.ic_char_lamp, "objetos"),
        GameIcon("crystal", "Cristal", R.drawable.ic_char_crystal, "objetos", "cristal,magico"),
        GameIcon("magnifying_glass", "Lupa", R.drawable.ic_char_magnifying_glass, "objetos", "detective,lupa"),
        GameIcon("potion", "Poción", R.drawable.ic_char_potion, "objetos", "magica,elixir"),
        GameIcon("violin", "Violín", R.drawable.ic_char_violin, "objetos", "musica"),
        GameIcon("moon", "Luna", R.drawable.ic_char_moon, "naturaleza"),
        GameIcon("sun", "Sol", R.drawable.ic_char_sun, "naturaleza"),
        GameIcon("star", "Estrella", R.drawable.ic_char_star, "naturaleza"),
        GameIcon("hearts", "Corazón", R.drawable.ic_char_hearts, "naturaleza")
    )

    val byKey: Map<String, GameIcon> = icons.associateBy { it.key }

    fun forCategory(category: String): List<GameIcon> = icons.filter { it.category == category }

    fun search(query: String): List<GameIcon> {
        val q = query.trim().lowercase()
        if (q.isEmpty()) return icons
        return icons.filter { icon ->
            icon.label.lowercase().contains(q) ||
                icon.category.lowercase().contains(q) ||
                (categoryNames[icon.category] ?: "").lowercase().contains(q) ||
                icon.tags.lowercase().contains(q)
        }
    }
}
