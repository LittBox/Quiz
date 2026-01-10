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
        <span class="question-hint" v-if="isMultiChoice">
          (请选择 {{ minSelectCount }}-{{ maxSelectCount }} 个答案)
        </span>
      </div>
      
      <!-- 题目内容 -->
      <div class="question-content" v-html="formatContent(currentQuestion.content)"></div>
      
      <!-- 多选题选择提示 -->
      <div v-if="isMultiChoice && !showAnswer" class="multi-choice-hint">
        <span class="hint-text">多选题：已选择 {{ selectedOptions.length }} 个答案</span>
        <span v-if="selectedOptions.length < minSelectCount" class="hint-warning">
          (至少选择 {{ minSelectCount }} 个)
        </span>
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
      currentQuestion: null,
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
      const answer = this.currentQuestion.answer || ''
      return Math.max(2, answer.length)
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
        // 多选题：至少选2个，不超过选项总数
        return this.selectedOptions.length >= this.minSelectCount && 
               this.selectedOptions.length <= this.maxSelectCount
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
      
      // 回答正确自动跳转（多选题不自动跳转，让用户查看分析）
      if (this.isCorrect && !this.isMultiChoice && this.currentQuestion.id < this.totalQuestions) {
        setTimeout(() => {
          this.nextQuestion()
        }, 2000)
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
.question-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
  padding-bottom: 8px;
  border-bottom: 1px solid #e0e0e0;
}

.question-id {
  font-size: 16px;
  font-weight: bold;
  color: #333;
}

.question-type {
  padding: 4px 12px;
  border-radius: 4px;
  font-size: 12px;
  font-weight: bold;
}

.type-single {
  background-color: #e3f2fd;
  color: #1976d2;
}

.type-multi {
  background-color: #f3e5f5;
  color: #7b1fa2;
}

.question-content {
  font-size: 16px;
  line-height: 1.6;
  margin-bottom: 24px;
  padding: 16px;
  background-color: #f9f9f9;
  border-radius: 8px;
  border-left: 4px solid #2196f3;
}

.options-container {
  margin: 20px 0;
}

.option-item {
  margin-bottom: 12px;
  padding: 12px 16px;
  border: 2px solid #e0e0e0;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.3s ease;
}

.option-item:hover {
  border-color: #64b5f6;
  background-color: #f5f5f5;
  transform: translateY(-2px);
}

.option-header {
  display: flex;
  align-items: center;
}

.option-radio {
  margin-right: 12px;
  cursor: pointer;
  transform: scale(1.2);
}

.option-key {
  font-weight: bold;
  margin-right: 8px;
  min-width: 20px;
  color: #1976d2;
}

.option-text {
  flex: 1;
  line-height: 1.5;
}

.correct-option {
  border-color: #4caf50;
  background-color: #e8f5e9;
  box-shadow: 0 2px 4px rgba(76, 175, 80, 0.2);
}

.wrong-option {
  border-color: #f44336;
  background-color: #ffebee;
  box-shadow: 0 2px 4px rgba(244, 67, 54, 0.2);
}

.selected-option {
  border-color: #2196f3;
  background-color: #e3f2fd;
}

.action-buttons {
  display: flex;
  gap: 12px;
  margin: 24px 0;
  flex-wrap: wrap;
}

button {
  padding: 10px 24px;
  border: none;
  border-radius: 6px;
  font-size: 14px;
  font-weight: bold;
  cursor: pointer;
  transition: all 0.3s ease;
  min-width: 100px;
}

button:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.btn-submit {
  background-color: #2196f3;
  color: white;
  box-shadow: 0 2px 4px rgba(33, 150, 243, 0.3);
}

.btn-submit:hover:not(:disabled) {
  background-color: #1976d2;
  transform: translateY(-2px);
  box-shadow: 0 4px 8px rgba(33, 150, 243, 0.3);
}

.btn-next {
  background-color: #4caf50;
  color: white;
  box-shadow: 0 2px 4px rgba(76, 175, 80, 0.3);
}

.btn-next:hover:not(:disabled) {
  background-color: #388e3c;
  transform: translateY(-2px);
  box-shadow: 0 4px 8px rgba(76, 175, 80, 0.3);
}

.btn-prev {
  background-color: #ff9800;
  color: white;
  box-shadow: 0 2px 4px rgba(255, 152, 0, 0.3);
}

.btn-prev:hover:not(:disabled) {
  background-color: #f57c00;
  transform: translateY(-2px);
  box-shadow: 0 4px 8px rgba(255, 152, 0, 0.3);
}

.btn-retry {
  background-color: #9c27b0;
  color: white;
  box-shadow: 0 2px 4px rgba(156, 39, 176, 0.3);
}

.btn-retry:hover:not(:disabled) {
  background-color: #7b1fa2;
  transform: translateY(-2px);
  box-shadow: 0 4px 8px rgba(156, 39, 176, 0.3);
}

.answer-section {
  margin-top: 24px;
  padding: 20px;
  background: linear-gradient(135deg, #f5f7fa 0%, #c3cfe2 100%);
  border-radius: 8px;
  border-left: 4px solid #4caf50;
}

.answer-result {
  font-size: 18px;
  font-weight: bold;
  margin-bottom: 16px;
  padding: 12px 20px;
  border-radius: 6px;
  display: inline-block;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
}

.answer-result.correct {
  background: linear-gradient(135deg, #4caf50 0%, #2e7d32 100%);
  color: white;
}

.answer-result.wrong {
  background: linear-gradient(135deg, #f44336 0%, #c62828 100%);
  color: white;
}

.correct-answer,
.user-answer,
.explanation {
  margin: 12px 0;
  line-height: 1.6;
  padding: 8px 12px;
  background-color: white;
  border-radius: 4px;
}

.question-nav {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 24px;
  padding: 16px;
  background-color: #f9f9f9;
  border-radius: 8px;
}

.nav-btn {
  padding: 10px 20px;
  background: linear-gradient(135deg, #6a11cb 0%, #2575fc 100%);
  color: white;
  font-weight: bold;
  box-shadow: 0 2px 4px rgba(106, 17, 203, 0.3);
}

.nav-btn:hover:not(:disabled) {
  transform: translateY(-2px);
  box-shadow: 0 4px 8px rgba(106, 17, 203, 0.3);
}

.nav-info {
  font-size: 14px;
  color: #666;
  display: flex;
  align-items: center;
  gap: 8px;
}

.jump-input {
  width: 60px;
  padding: 4px 8px;
  border: 1px solid #ddd;
  border-radius: 4px;
  text-align: center;
  font-weight: bold;
}

.quick-jump {
  margin-top: 16px;
  padding: 12px;
  background-color: #f0f0f0;
  border-radius: 8px;
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  align-items: center;
}

.quick-jump-label {
  font-size: 14px;
  color: #666;
  font-weight: bold;
}

.quick-jump-btn {
  padding: 6px 12px;
  background-color: white;
  border: 1px solid #ddd;
  border-radius: 4px;
  font-size: 12px;
  cursor: pointer;
  transition: all 0.3s ease;
}

.quick-jump-btn:hover {
  background-color: #e3f2fd;
  border-color: #2196f3;
}

.quick-jump-btn.active {
  background-color: #2196f3;
  color: white;
  border-color: #2196f3;
}

@media (max-width: 768px) {
  .question-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 8px;
  }
  
  .action-buttons {
    flex-direction: column;
  }
  
  button {
    width: 100%;
  }
  
  .question-nav {
    flex-direction: column;
    gap: 12px;
  }
  
  .quick-jump {
    flex-direction: column;
    align-items: flex-start;
  }
}

.question-hint {
  font-size: 12px;
  color: #666;
  margin-left: 10px;
}

.multi-choice-hint {
  margin: 12px 0;
  padding: 10px 16px;
  background-color: #e8f4fd;
  border-left: 4px solid #2196f3;
  border-radius: 4px;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.hint-text {
  font-size: 14px;
  color: #1976d2;
  font-weight: bold;
}

.hint-warning {
  font-size: 12px;
  color: #f57c00;
  font-weight: bold;
}

/* 多选题答案样式 */
.multi-answer {
  font-weight: bold;
  color: #1976d2;
  margin-left: 4px;
}

/* 多选题分析样式 */
.answer-analysis {
  margin-top: 20px;
  padding: 16px;
  background-color: #f9f9f9;
  border-radius: 8px;
  border: 1px solid #e0e0e0;
}

.analysis-title {
  font-weight: bold;
  margin-bottom: 12px;
  color: #333;
  font-size: 16px;
}

.analysis-item {
  display: flex;
  align-items: flex-start;
  margin-bottom: 8px;
  padding: 8px;
  border-radius: 4px;
  background-color: white;
}

.analysis-option {
  font-weight: bold;
  min-width: 20px;
  margin-right: 8px;
}

.analysis-text {
  flex: 1;
  margin-right: 12px;
  font-size: 14px;
}

.analysis-status {
  font-size: 12px;
  padding: 2px 8px;
  border-radius: 12px;
  font-weight: bold;
  white-space: nowrap;
}

.analysis-status.correct {
  background-color: #e8f5e9;
  color: #2e7d32;
}

.analysis-status.missed {
  background-color: #fff3e0;
  color: #f57c00;
}

.analysis-status.wrong {
  background-color: #ffebee;
  color: #c62828;
}

.analysis-status.neutral {
  background-color: #f5f5f5;
  color: #757575;
}

/* 复选框样式 */
.option-checkbox {
  margin-right: 12px;
  cursor: pointer;
  transform: scale(1.2);
}

.option-checkbox:disabled,
.option-radio:disabled {
  cursor: not-allowed;
  opacity: 0.6;
}
</style>