<template>
  <div>
    <h2>上传题库（.docx / .pdf）</h2>
    <input type="file" @change="onFileChange" />
    <button @click="upload" :disabled="!file">上传</button>
    <div v-if="status">{{ status }}</div>
  </div>
</template>

<script>
import axios from 'axios'
export default {
  data() { return { file: null, status: null } },
  methods: {
    onFileChange(e) { this.file = e.target.files[0] },
    
    async upload() {
      if (!this.file) return;
      const fd = new FormData();
      fd.append('file', this.file);
      this.status = 'Uploading...';
      try {
        const res = await axios.post('http://localhost:8080/api/upload', fd, { headers: { 'Content-Type': 'multipart/form-data' } });
        this.status = res.data;
      } catch (e) {
        this.status = 'Upload failed: ' + (e.response?.data || e.message);
      }
    }
  }
}
</script>
