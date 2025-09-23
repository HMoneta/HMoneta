<template>
  <v-card
    class="mx-auto"
    max-width="400"
  >
    <v-img
      class="align-end text-white"
      height="200"
      src="https://cdn.vuetifyjs.com/images/cards/docks.jpg"
      cover
    >
      <v-card-title>登陆HMoneta系统</v-card-title>
    </v-img>

    <v-card-text>
      <v-text-field label="用户名" variant="underlined" v-model="loginInfo.username" clearable></v-text-field>

      <v-text-field
        label="密码"
        variant="underlined"
        v-model="loginInfo.password"
        :type="visible ? 'text' : 'password'"
        :append-inner-icon="visible ? 'mdi-eye-off' : 'mdi-eye'"
        @click:append-inner="visible = !visible"
        clearable></v-text-field>
    </v-card-text>

    <v-card-actions>
      <v-btn color="orange" text="登陆" @click="login"></v-btn>
    </v-card-actions>
  </v-card>
</template>

<script setup lang="js">
import {http} from "@/common/request.js";

const visible = ref(false)
const loginInfo = reactive({
  username: "",
  password: "",
})

const login = async () => {
  console.log(loginInfo);
  const json = await http.post("/user/login", loginInfo);
  console.log(json)
  return json;
}
</script>


<style scoped lang="sass">

</style>
