<template>
  <div>
    <h2>做题练习</h2>
    <div v-if="loading">加载题目中...</div>
    <div v-else-if="!currentQuestion">暂无题目，请联系管理员添加题库</div>
    <div v-else>
      <!-- 题目信息 -->
      <div class="question-header">
        <span class="question-id">第 {{ currentQuestion.id }} 题</span>
        <span class="question-type" :class="questionTypeClass">
          {{ questionTypeText }}
        </span>
      </div>
      
      <!-- 题目内容 -->
      <div class="question-content" v-html="formatContent(currentQuestion.content)"></div>
      
      <!-- 多选题选择提示 -->
      <div v-if="isMultiChoice && !showAnswer" class="multi-choice-hint">
        <span class="hint-text">多选题：已选择 {{ selectedOptions.length }} 个答案</span>
      </div>
      
      <!-- 选项 -->
      <div class="options-container">
        <div 
          v-for="option in options" 
          :key="option.key"
          class="option-item"
          :class="getOptionClass(option.key)"
          @click="selectOption(option.key)"
        >
          <div class="option-header">
            <!-- 单选题使用radio，多选题使用checkbox -->
            <input 
              v-if="isMultiChoice"
              type="checkbox" 
              :value="option.key" 
              v-model="selectedOptions"
              :disabled="showAnswer"
              class="option-checkbox"
              @click.stop
            />
            <input 
              v-else
              type="radio" 
              :value="option.key" 
              v-model="selectedOption"
              :disabled="showAnswer"
              class="option-radio"
              @click.stop
            />
            <span class="option-key">{{ option.key }}.</span>
            <span class="option-text">{{ option.text }}</span>
          </div>
        </div>
      </div>
      
      <!-- 操作按钮 -->
      <div class="action-buttons">
        <button 
          @click="submitAnswer" 
          :disabled="!canSubmit || showAnswer"
          class="btn-submit"
        >
          {{ isMultiChoice ? '提交多选题' : '提交答案' }}
        </button>
        <button 
          @click="nextQuestion" 
          v-if="showAnswer"
          class="btn-next"
        >
          下一题
        </button>
        <button 
          @click="prevQuestion" 
          v-if="showAnswer && currentQuestion.id > 1"
          class="btn-prev"
        >
          上一题
        </button>
        <button 
          @click="retryQuestion" 
          v-if="showAnswer"
          class="btn-retry"
        >
          重做本题
        </button>
      </div>
      
      <!-- 答案解析 -->
      <div v-if="showAnswer" class="answer-section">
        <div class="answer-result" :class="isCorrect ? 'correct' : 'wrong'">
          {{ isCorrect ? '回答正确！' : '回答错误！' }}
        </div>
        
        <!-- 多选题答案格式化显示 -->
        <div class="correct-answer">
          <strong>正确答案：</strong>
          <span v-if="isMultiChoice" class="multi-answer">
            {{ formatMultiAnswer(currentQuestion.answer) }}
          </span>
          <span v-else>
            {{ currentQuestion.answer }}
          </span>
        </div>
        
        <div v-if="!isCorrect" class="user-answer">
          <strong>你的答案：</strong>
          <span v-if="isMultiChoice" class="multi-answer">
            {{ formatUserMultiAnswer() }}
          </span>
          <span v-else>
            {{ selectedOption || '未选择' }}
          </span>
        </div>
        
        <!-- 多选题详细分析 -->
        <div v-if="isMultiChoice && !isCorrect" class="answer-analysis">
          <div class="analysis-title">答案分析：</div>
          <div v-for="option in options" :key="option.key" class="analysis-item">
            <span class="analysis-option">{{ option.key }}.</span>
            <span class="analysis-text">{{ option.text }}</span>
            <span class="analysis-status" :class="getAnalysisStatus(option.key)">
              {{ getAnalysisStatusText(option.key) }}
            </span>
          </div>
        </div>
        
        <div v-if="currentQuestion.explanation" class="explanation">
          <strong>解析：</strong><span v-html="currentQuestion.explanation"/>
        </div>
      </div>
      
      <!-- 题目导航 -->
      <div class="question-nav">
        <button 
          @click="jumpToQuestion(currentQuestion.id - 1)"
          :disabled="currentQuestion.id <= 1"
          class="nav-btn"
        >
          ← 上一题
        </button>
        <span class="nav-info">
          第 <input 
            type="number" 
            v-model="jumpId" 
            @keyup.enter="jumpToInputQuestion"
            min="1" 
            :max="totalQuestions" 
            class="jump-input"
          > 题 / 共 {{ totalQuestions }} 题
        </span>
        <button 
          @click="jumpToQuestion(currentQuestion.id + 1)"
          :disabled="currentQuestion.id >= totalQuestions"
          class="nav-btn"
        >
          下一题 →
        </button>
      </div>

      <!-- 快速跳转 -->
      <div class="quick-jump" v-if="totalQuestions > 10">
        <span class="quick-jump-label">快速跳转：</span>
        <button 
          v-for="page in quickJumpPages" 
          :key="page"
          @click="jumpToQuestion((page - 1) * 10 + 1)"
          class="quick-jump-btn"
          :class="{ active: currentQuestion.id >= (page - 1) * 10 + 1 && currentQuestion.id <= page * 10 }"
        >
          {{ (page - 1) * 10 + 1 }}-{{ Math.min(page * 10, totalQuestions) }}
        </button>
      </div>
    </div>
  </div>
</template>

<script>
import axios from 'axios'

export default {
  name: 'QuestionPractice',
  data() {
    return {
      loading: true,
      currentQuestion: null,    // 当前题目对象
      selectedOption: null,      // 单选题答案
      selectedOptions: [],       // 多选题答案（数组）
      showAnswer: false,
      isCorrect: false,
      totalQuestions: 0,
      jumpId: 1,
      questionIds: []
    }
  },
  computed: {
    // 是否是多选题
    isMultiChoice() {
      return this.currentQuestion && 
             this.currentQuestion.questionType === 'multi_choice'
    },
    
    questionTypeClass() {
      if (!this.currentQuestion) return ''
      return this.isMultiChoice ? 'type-multi' : 'type-single'
    },
    
    questionTypeText() {
      if (!this.currentQuestion) return ''
      return this.isMultiChoice ? '多选题' : '单选题'
    },
    
    // 多选题最少选择数量（至少2个）
    minSelectCount() {
      if (!this.isMultiChoice) return 1
      return 2
    },
    
    // 多选题最多选择数量
    maxSelectCount() {
      if (!this.isMultiChoice) return 1
      return this.options.length
    },
    
    // 是否可以提交
    canSubmit() {
      if (this.showAnswer) return false
      
      if (this.isMultiChoice) {
        // 多选题：至少选2个
        return this.selectedOptions.length >= this.minSelectCount 
      } else {
        // 单选题：必须选1个
        return this.selectedOption !== null
      }
    },
    
    options() {
      if (!this.currentQuestion) return []
      const opts = []
      const addOption = (key, text) => {
        if (text && text.trim()) {
          opts.push({ key, text: text.trim() })
        }
      }
      
      // 添加A-D选项
      addOption('A', this.currentQuestion.optionA)
      addOption('B', this.currentQuestion.optionB)
      addOption('C', this.currentQuestion.optionC)
      addOption('D', this.currentQuestion.optionD)
      
      // 如果有E选项
      if (this.currentQuestion.optionE && this.currentQuestion.optionE.trim()) {
        addOption('E', this.currentQuestion.optionE)
      }
      
      // 如果有F选项（如果需要的话）
      if (this.currentQuestion.optionF && this.currentQuestion.optionF.trim()) {
        addOption('F', this.currentQuestion.optionF)
      }
      
      return opts
    },
    
    quickJumpPages() {
      if (this.totalQuestions <= 0) return []
      const pageCount = Math.ceil(this.totalQuestions / 10)
      return Array.from({ length: pageCount }, (_, i) => i + 1)
    }
  },
  watch: {
    'currentQuestion.id': {
      handler(newId) {
        if (newId) {
          this.jumpId = newId
        }
      },
      immediate: true
    },
    
    // 监听题目变化，重置答案
    currentQuestion() {
      this.resetAnswer()
    }
  },
  mounted() {
    this.initPractice()
  },
  methods: {
    async initPractice() {
      await this.loadTotalQuestions()
      await this.loadQuestionIds()
      await this.loadQuestion(1)
    },
    
    async loadTotalQuestions() {
      try {
        const res = await axios.get('http://localhost:8080/api/questions/count')
        this.totalQuestions = this.extractCount(res.data)
        console.log('获取到题目总数:', this.totalQuestions)
      } catch (error) {
        console.error('获取题目总数失败:', error)
        this.totalQuestions = 0
      }
    },
    
    extractCount(data) {
      if (typeof data === 'number') return data
      if (data && typeof data === 'object') {
        if (data.count !== undefined) return data.count
        if (data.total !== undefined) return data.total
      }
      return 0
    },
    
    async loadQuestionIds() {
      try {
        const res = await axios.get('http://localhost:8080/api/questions/ids')
        if (res.data && Array.isArray(res.data)) {
          this.questionIds = res.data
        }
      } catch (error) {
        if (this.totalQuestions > 0) {
          this.questionIds = Array.from({ length: this.totalQuestions }, (_, i) => i + 1)
        }
      }
    },
    
    async loadQuestion(id) {
      this.loading = true
      this.resetAnswer()
      
      try {
        const validId = Math.max(1, Math.min(id, this.totalQuestions))
        console.log(`正在加载题目 ID: ${validId}`)
        const res = await axios.get(`http://localhost:8080/api/questions/${validId}`)
        
        if (res.data) {
          this.currentQuestion = res.data
          console.log(`题目 ${validId} 加载成功，类型：${this.questionTypeText}`)
          if (this.isMultiChoice) {
            console.log(`多选题答案：${this.currentQuestion.answer}，选项数量：${this.options.length}`)
          }
        } else {
          this.currentQuestion = null
        }
      } catch (error) {
        console.error('加载题目失败:', error)
        this.currentQuestion = null
        if (id !== 1) await this.loadQuestion(1)
      } finally {
        this.loading = false
      }
    },
    
    resetAnswer() {
      this.selectedOption = null
      this.selectedOptions = []
      this.showAnswer = false
      this.isCorrect = false
    },
    
    formatContent(content) {
      if (!content) return ''
      return content.replace(/\n/g, '<br>')
    },
   
    
    selectOption(optionKey) {
      if (this.showAnswer) return
      
      if (this.isMultiChoice) {
        // 多选题：切换选中状态
        const index = this.selectedOptions.indexOf(optionKey)
        if (index === -1) {
          this.selectedOptions.push(optionKey)
        } else {
          this.selectedOptions.splice(index, 1)
        }
      } else {
        // 单选题：直接选中
        this.selectedOption = optionKey
      }
    },
    
    submitAnswer() {
      if (!this.canSubmit) {
        if (this.isMultiChoice) {
          if (this.selectedOptions.length < this.minSelectCount) {
            alert(`多选题至少需要选择 ${this.minSelectCount} 个答案`)
          } else if (this.selectedOptions.length > this.maxSelectCount) {
            alert(`多选题最多只能选择 ${this.maxSelectCount} 个答案`)
          }
        } else {
          alert('请选择一个答案')
        }
        return
      }
      
      this.showAnswer = true
      
      if (this.isMultiChoice) {
        // 多选题：比较排序后的答案（忽略顺序）
        const userAnswer = [...this.selectedOptions].sort().join('')
        const correctAnswer = (this.currentQuestion.answer || '').split('').sort().join('')
        this.isCorrect = userAnswer === correctAnswer
        
        console.log(`多选题提交：用户答案=${userAnswer}，正确答案=${correctAnswer}，正确=${this.isCorrect}`)
      } else {
        // 单选题：直接比较
        this.isCorrect = this.selectedOption === this.currentQuestion.answer
      }
      
    },
    
    nextQuestion() {
      if (this.currentQuestion && this.currentQuestion.id < this.totalQuestions) {
        this.loadQuestion(this.currentQuestion.id + 1)
      }
    },
    
    prevQuestion() {
      if (this.currentQuestion && this.currentQuestion.id > 1) {
        this.loadQuestion(this.currentQuestion.id - 1)
      }
    },
    //retry ：动词，英文意思「重试、重新做、再试一次」
    retryQuestion() {
      this.resetAnswer()
    },
    
    jumpToQuestion(id) {
      if (id >= 1 && id <= this.totalQuestions) {
        this.loadQuestion(id)
      }
    },
    
    jumpToInputQuestion() {
      const id = parseInt(this.jumpId)
      if (!isNaN(id) && id >= 1 && id <= this.totalQuestions) {
        this.jumpToQuestion(id)
      } else {
        alert(`请输入有效的题目ID (1-${this.totalQuestions})`)
      }
    },
    
    getOptionClass(optionKey) {
      if (!this.showAnswer) {
        // 未提交时：显示选中状态
        if (this.isMultiChoice) {
          return this.selectedOptions.includes(optionKey) ? 'selected-option' : ''
        } else {
          return this.selectedOption === optionKey ? 'selected-option' : ''
        }
      }
      
      // 提交后：显示对错状态
      const classes = []
      const correctAnswer = this.currentQuestion.answer || ''
      const isCorrectOption = correctAnswer.includes(optionKey)
      
      if (this.isMultiChoice) {
        const isSelected = this.selectedOptions.includes(optionKey)
        if (isCorrectOption) {
          classes.push('correct-option')
        }
        if (isSelected && !isCorrectOption) {
          classes.push('wrong-option')
        }
        if (isSelected) {
          classes.push('selected-option')
        }
      } else {
        if (optionKey === this.currentQuestion.answer) {
          classes.push('correct-option')
        }
        if (optionKey === this.selectedOption && optionKey !== this.currentQuestion.answer) {
          classes.push('wrong-option')
        }
        if (optionKey === this.selectedOption) {
          classes.push('selected-option')
        }
      }
      
      return classes.join(' ')
    },
    
    formatMultiAnswer(answer) {
      if (!answer) return '无'
      return answer.split('').join('、')
    },
    
    formatUserMultiAnswer() {
      if (this.selectedOptions.length === 0) return '未选择'
      return [...this.selectedOptions].sort().join('、')
    },
    
    getAnalysisStatus(optionKey) {
      const correctAnswer = this.currentQuestion.answer || ''
      const isCorrect = correctAnswer.includes(optionKey)
      const isSelected = this.selectedOptions.includes(optionKey)
      
      if (isCorrect && isSelected) return 'correct'
      if (isCorrect && !isSelected) return 'missed'
      if (!isCorrect && isSelected) return 'wrong'
      return 'neutral'
    },
    
    getAnalysisStatusText(optionKey) {
      const status = this.getAnalysisStatus(optionKey)
      switch (status) {
        case 'correct': return '✓ 正确选择'
        case 'missed': return '✗ 应选未选'
        case 'wrong': return '✗ 错误选择'
        default: return ''
      }
    }
  }
}
</script>

<style scoped>

</style>