package com.markedusduplicate.deckard.domain

import com.markedusduplicate.deckard.domain.model.DomainTodo
import com.markedusduplicate.deckard.net.model.ApiTodo

class TodoMapper {

    fun map(apiTodo: ApiTodo?): DomainTodo {
        return DomainTodo(
            id = apiTodo!!.id!!,
            title = apiTodo.title!!,
            completed = apiTodo.completed!!
        )
    }
}
