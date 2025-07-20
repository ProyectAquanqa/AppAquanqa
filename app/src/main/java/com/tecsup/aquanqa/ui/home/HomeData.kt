package com.tecsup.aquanqa.ui.home

data class Category(
    val name: String,
    val iconResId: Int
)

data class Author(
    val name: String,
    val imageUrl: String // Usaremos URLs para las imágenes de perfil
)

data class TeamMember(
    val name: String,
    val imageUrl: String
)

data class Publication(
    val title: String,
    val iconResId: Int,
    val author: Author,
    val date: String,
    val content: String,
    val team: List<TeamMember>
) 